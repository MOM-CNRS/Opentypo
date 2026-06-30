package fr.cnrs.opentypo.application.import_typology;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Locale;

/**
 * Vérifie qu'une URL distante pointe vers une image accessible (HEAD puis GET si besoin).
 */
@Slf4j
public final class RemoteImageUrlValidator {

    private RemoteImageUrlValidator() {
    }

    /**
     * Validation sans accès réseau (schéma, hôte, extension plausible).
     * Utilisée à l'import lorsque l'URL a déjà été validée à l'analyse ou est nouvelle.
     */
    public static boolean isSyntaxValidUrl(String rawUrl) {
        URI uri = parseHttpUri(rawUrl);
        if (uri == null) {
            return false;
        }
        String path = uri.getPath();
        return isLikelyImagePath(path) || !StringUtils.hasText(path) || "/".equals(path);
    }

    public static boolean isValidRemoteImageUrl(String rawUrl) {
        URI uri = parseHttpUri(rawUrl);
        if (uri == null) {
            return false;
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("HEAD");

            int status = connection.getResponseCode();
            if (status < 200 || status >= 400) {
                if (status == HttpURLConnection.HTTP_BAD_METHOD) {
                    return isValidRemoteImageUrlWithGet(uri);
                }
                return false;
            }
            String contentType = connection.getContentType();
            if (isImageContentType(contentType)) {
                return true;
            }
            return isLikelyImagePath(uri.getPath());
        } catch (IOException e) {
            log.debug("URL image inaccessible: {}", rawUrl);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static boolean isValidRemoteImageUrlWithGet(URI uri) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Range", "bytes=0-1024");

            int status = connection.getResponseCode();
            if (status < 200 || status >= 400) {
                return false;
            }

            String contentType = connection.getContentType();
            if (isImageContentType(contentType)) {
                return true;
            }
            return isLikelyImagePath(uri.getPath());
        } catch (IOException e) {
            log.debug("URL image inaccessible (GET): {}", uri);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static boolean isImageContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
        return normalized.startsWith("image/");
    }

    private static URI parseHttpUri(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            return null;
        }
        if (!StringUtils.hasText(uri.getHost())) {
            return null;
        }
        return uri;
    }

    private static boolean isLikelyImagePath(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        String lower = path.toLowerCase(Locale.ENGLISH);
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".svg")
                || lower.endsWith(".bmp");
    }

}
