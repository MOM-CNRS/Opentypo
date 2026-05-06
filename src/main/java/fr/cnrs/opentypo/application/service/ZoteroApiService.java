package fr.cnrs.opentypo.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentypo.application.dto.zotero.ZoteroSearchHit;
import fr.cnrs.opentypo.infrastructure.config.ZoteroProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
/**
 * Appels lecture seule à l'API Zotero v3 (groupe partagé).
 *
 * @see <a href="https://www.zotero.org/support/dev/web_api/v3/start">Zotero Web API v3</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZoteroApiService {

    private static final String API_ROOT = "https://api.zotero.org";
    private static final int MAX_ITEM_KEYS_PER_REQUEST = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(12);

    private final ZoteroProperties properties;
    /** Instance locale : pas de bean {@code ObjectMapper} enregistré par défaut dans ce projet. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    public List<String> parseItemKeysJson(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            JsonNode arr = objectMapper.readTree(json.trim());
            if (!arr.isArray()) {
                return List.of();
            }
            List<String> out = new ArrayList<>();
            for (JsonNode n : arr) {
                if (n.isTextual()) {
                    String k = n.asText().trim();
                    if (!k.isEmpty()) {
                        out.add(k);
                    }
                }
            }
            return out;
        } catch (Exception e) {
            log.debug("JSON Zotero invalide: {}", e.getMessage());
            return List.of();
        }
    }

    public String serializeItemKeysJson(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String k : keys) {
            if (StringUtils.hasText(k)) {
                unique.add(k.trim());
            }
        }
        if (unique.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(new ArrayList<>(unique));
        } catch (Exception e) {
            log.warn("Sérialisation clés Zotero: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Recherche rapide dans la bibliothèque groupe (titres / créateurs).
     */
    public List<ZoteroSearchHit> searchTopLevelItems(String query, int limit) {
        if (!StringUtils.hasText(query) || query.trim().length() < 2) {
            return List.of();
        }
        int lim = Math.min(Math.max(limit, 1), 25);
        String q = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String uri = API_ROOT + "/groups/" + properties.getGroupId() + "/items/top?q=" + q
                + "&limit=" + lim + "&format=json";
        Optional<String> body = httpGet(uri);
        if (body.isEmpty()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(body.get());
            if (!root.isArray()) {
                return List.of();
            }
            List<ZoteroSearchHit> hits = new ArrayList<>();
            for (JsonNode node : root) {
                JsonNode data = node.get("data");
                if (data == null || !data.isObject()) {
                    continue;
                }
                String itemType = text(data, "itemType");
                if ("attachment".equals(itemType) || "note".equals(itemType)) {
                    continue;
                }
                String key = text(node, "key");
                if (!StringUtils.hasText(key)) {
                    continue;
                }
                String title = firstTitle(data);
                String label = buildLabel(title, data);
                hits.add(new ZoteroSearchHit(key, label));
            }
            return hits;
        } catch (IOException e) {
            log.debug("Parse recherche Zotero: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Bibliographie formatée (XHTML) pour les clés données, dans l'ordre des lots API.
     */
    public String fetchBibliographyHtml(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return "";
        }
        List<String> batches = batch(keys, MAX_ITEM_KEYS_PER_REQUEST);
        StringBuilder sb = new StringBuilder();
        for (String batch : batches) {
            String uri = API_ROOT + "/groups/" + properties.getGroupId() + "/items?itemKey="
                    + URLEncoder.encode(batch, StandardCharsets.UTF_8)
                    + "&format=bib&linkwrap=1"
                    + "&style=" + URLEncoder.encode(properties.getCitationStyle(), StandardCharsets.UTF_8)
                    + "&locale=" + URLEncoder.encode(properties.getLocale(), StandardCharsets.UTF_8);
            Optional<String> html = httpGet(uri);
            html.ifPresent(sb::append);
        }
        return sb.toString().trim();
    }

    /** Libellés courts pour recharger l'éditeur (batch JSON), ordre conservé. */
    public List<ZoteroSearchHit> resolveLabels(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<ZoteroSearchHit> out = new ArrayList<>();
        for (List<String> part : partition(keys, MAX_ITEM_KEYS_PER_REQUEST)) {
            String joined = String.join(",", part);
            String uri = API_ROOT + "/groups/" + properties.getGroupId() + "/items?itemKey="
                    + URLEncoder.encode(joined, StandardCharsets.UTF_8) + "&limit=" + MAX_ITEM_KEYS_PER_REQUEST + "&format=json";
            Optional<String> body = httpGet(uri);
            Map<String, ZoteroSearchHit> byKey = new HashMap<>();
            if (body.isPresent()) {
                try {
                    JsonNode root = objectMapper.readTree(body.get());
                    if (root.isArray()) {
                        for (JsonNode node : root) {
                            String key = text(node, "key");
                            if (!StringUtils.hasText(key)) {
                                continue;
                            }
                            JsonNode data = node.get("data");
                            if (data != null && data.isObject()) {
                                String title = firstTitle(data);
                                byKey.put(key, new ZoteroSearchHit(key, buildLabel(title, data)));
                            } else {
                                byKey.put(key, new ZoteroSearchHit(key, key));
                            }
                        }
                    }
                } catch (IOException e) {
                    log.debug("Parse labels Zotero: {}", e.getMessage());
                }
            }
            for (String k : part) {
                out.add(byKey.getOrDefault(k, new ZoteroSearchHit(k, k)));
            }
        }
        return out;
    }

    public long getConfiguredGroupId() {
        return properties.getGroupId();
    }

    private Optional<String> httpGet(String uri) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(TIMEOUT)
                    .header("Zotero-API-Version", "3")
                    .GET();
            if (StringUtils.hasText(properties.getApiKey())) {
                b.header("Zotero-API-Key", properties.getApiKey().trim());
            }
            HttpResponse<String> resp = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return Optional.ofNullable(resp.body());
            }
            log.debug("Zotero HTTP {} pour {}", resp.statusCode(), uri);
        } catch (Exception e) {
            log.debug("Zotero requête échouée: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private static String text(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return n != null && n.isTextual() ? n.asText() : "";
    }

    private static String firstTitle(JsonNode data) {
        JsonNode t = data.get("title");
        if (t != null && t.isTextual() && StringUtils.hasText(t.asText())) {
            return t.asText().trim();
        }
        return "";
    }

    private static String buildLabel(String title, JsonNode data) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(title)) {
            sb.append(title);
        } else {
            sb.append("(sans titre)");
        }
        String creator = firstCreator(data);
        if (StringUtils.hasText(creator)) {
            sb.append(" — ").append(creator);
        }
        String date = text(data, "date");
        if (StringUtils.hasText(date)) {
            sb.append(" (").append(date.trim()).append(")");
        }
        return sb.toString();
    }

    private static String firstCreator(JsonNode data) {
        JsonNode creators = data.get("creators");
        if (creators == null || !creators.isArray() || creators.isEmpty()) {
            return "";
        }
        JsonNode c = creators.get(0);
        if (c == null || !c.isObject()) {
            return "";
        }
        String last = text(c, "lastName");
        String first = text(c, "firstName");
        if (StringUtils.hasText(last) || StringUtils.hasText(first)) {
            return (first + " " + last).trim();
        }
        String name = text(c, "name");
        return name != null ? name.trim() : "";
    }

    private static List<String> batch(List<String> keys, int max) {
        if (keys.isEmpty()) {
            return List.of();
        }
        List<String> batches = new ArrayList<>();
        for (List<String> part : partition(keys, max)) {
            batches.add(String.join(",", part));
        }
        return batches;
    }

    private static List<List<String>> partition(List<String> keys, int size) {
        List<List<String>> out = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        for (String k : keys) {
            if (!StringUtils.hasText(k)) {
                continue;
            }
            cur.add(k.trim());
            if (cur.size() >= size) {
                out.add(cur);
                cur = new ArrayList<>();
            }
        }
        if (!cur.isEmpty()) {
            out.add(cur);
        }
        return out;
    }
}
