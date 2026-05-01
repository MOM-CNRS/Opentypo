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

    public static boolean isValidRemoteImageUrl(String rawUrl) {
        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            return false;
        }
        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
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
