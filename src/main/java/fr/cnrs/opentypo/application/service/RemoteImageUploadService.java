package fr.cnrs.opentypo.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Upload d’images vers un serveur distant (HTTP) configurable.
 */
@Service
@Slf4j
public class RemoteImageUploadService {

    @Value("${opentypo.remote-image-upload.base-url:http://localhost:8182}")
    private String serverBaseUrl;

    @Value("${opentypo.remote-image-upload.api-key:}")
    private String apiKey;

    @Value("${opentypo.remote-image-upload.path:/api/upload}")
    private String uploadPath;

    private final RestTemplate restTemplate;

    public RemoteImageUploadService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Envoie une image au serveur configuré et retourne l’URL publique renvoyée (ou dérivée).
     */
    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide ou null");
        }

        String base = serverBaseUrl.endsWith("/") ? serverBaseUrl.substring(0, serverBaseUrl.length() - 1) : serverBaseUrl;
        log.info("Upload de l'image {} vers le serveur distant: {}", file.getOriginalFilename(), base);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + apiKey);
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            body.add("file", resource);
            body.add("filename", file.getOriginalFilename());
            body.add("contentType", file.getContentType());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String path = uploadPath.startsWith("/") ? uploadPath : "/" + uploadPath;
            String uploadEndpoint = base + path;

            log.debug("Envoi de la requête POST vers: {}", uploadEndpoint);

            ResponseEntity<String> response = restTemplate.exchange(
                    uploadEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                String imageUrl = extractImageUrlFromResponse(response.getBody());
                log.info("Image uploadée avec succès. URL: {}", imageUrl);
                return imageUrl;
            } else {
                log.error("Échec de l'upload. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Échec de l'upload vers le serveur distant. Status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Erreur lors de l'upload de l'image vers le serveur distant", e);
            throw new RuntimeException("Erreur lors de l'upload: " + e.getMessage(), e);
        }
    }

    private String extractImageUrlFromResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new RuntimeException("Réponse vide du serveur distant");
        }

        if (responseBody.startsWith("http://") || responseBody.startsWith("https://")) {
            return responseBody.trim();
        }

        try {
            if (responseBody.contains("\"id\"")) {
                int idIndex = responseBody.indexOf("\"id\"");
                int startIndex = responseBody.indexOf("\"", idIndex + 4) + 1;
                int endIndex = responseBody.indexOf("\"", startIndex);
                if (startIndex > 0 && endIndex > startIndex) {
                    return responseBody.substring(startIndex, endIndex);
                }
            }
            if (responseBody.contains("\"@id\"")) {
                int idIndex = responseBody.indexOf("\"@id\"");
                int startIndex = responseBody.indexOf("\"", idIndex + 5) + 1;
                int endIndex = responseBody.indexOf("\"", startIndex);
                if (startIndex > 0 && endIndex > startIndex) {
                    return responseBody.substring(startIndex, endIndex);
                }
            }
        } catch (Exception e) {
            log.warn("Impossible d'extraire l'URL depuis la réponse JSON: {}", responseBody, e);
        }

        String base = serverBaseUrl.endsWith("/") ? serverBaseUrl.substring(0, serverBaseUrl.length() - 1) : serverBaseUrl;
        log.warn("Impossible d'extraire l'URL depuis la réponse. Utilisation d'une URL par défaut.");
        return base + "/media/" + System.currentTimeMillis() + ".jpg";
    }
}
