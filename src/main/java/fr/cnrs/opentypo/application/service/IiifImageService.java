package fr.cnrs.opentypo.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Service pour gérer l'upload d'images vers un serveur IIIF
 */
@Service
@Slf4j
public class IiifImageService {

    @Value("${iiif.server.url:http://localhost:8182}")
    private String iiifServerUrl;

    @Value("${iiif.server.api-key:}")
    private String iiifApiKey;

    private final RestTemplate restTemplate;

    public IiifImageService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Upload une image vers le serveur IIIF et retourne l'URL de l'image
     * 
     * @param file Le fichier image à uploader
     * @return L'URL de l'image sur le serveur IIIF
     * @throws IOException Si une erreur survient lors de la lecture du fichier
     * @throws RuntimeException Si l'upload échoue
     */
    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide ou null");
        }

        log.info("Upload de l'image {} vers le serveur IIIF: {}", file.getOriginalFilename(), iiifServerUrl);

        try {
            // Préparer les headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Ajouter la clé API si configurée
            if (iiifApiKey != null && !iiifApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + iiifApiKey);
            }

            // Préparer le body avec le fichier
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Convertir le fichier en ByteArrayResource pour l'upload
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            
            body.add("file", resource);
            body.add("filename", file.getOriginalFilename());
            body.add("contentType", file.getContentType());

            // Créer la requête
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Endpoint IIIF typique pour l'upload (peut varier selon votre serveur IIIF)
            // Exemples d'endpoints courants:
            // - /iiif/v3/upload (Cantaloupe)
            // - /api/v1/images (Cantaloupe alternative)
            // - /images (Cantaloupe simple)
            String uploadEndpoint = iiifServerUrl + "/iiif/v3/upload";
            
            log.debug("Envoi de la requête POST vers: {}", uploadEndpoint);

            // Envoyer la requête
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
                throw new RuntimeException("Échec de l'upload vers IIIF. Status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Erreur lors de l'upload de l'image vers IIIF", e);
            throw new RuntimeException("Erreur lors de l'upload vers IIIF: " + e.getMessage(), e);
        }
    }

    /**
     * Extrait l'URL de l'image depuis la réponse du serveur IIIF
     * La réponse peut être au format JSON avec un champ "id" ou directement l'URL
     * 
     * @param responseBody Le corps de la réponse du serveur IIIF
     * @return L'URL de l'image
     */
    private String extractImageUrlFromResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new RuntimeException("Réponse vide du serveur IIIF");
        }

        // Si la réponse est directement une URL
        if (responseBody.startsWith("http://") || responseBody.startsWith("https://")) {
            return responseBody.trim();
        }

        // Si la réponse est au format JSON, essayer d'extraire l'URL
        // Format typique: {"id": "https://iiif.example.com/iiif/3/image.jpg"}
        // ou {"@id": "https://iiif.example.com/iiif/3/image.jpg"}
        try {
            // Extraction simple basée sur des patterns JSON courants
            // Pour une extraction plus robuste, utiliser une bibliothèque JSON comme Jackson
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

        // Si on ne peut pas extraire l'URL, générer une URL basée sur le serveur IIIF
        // et un identifiant unique (fallback)
        log.warn("Impossible d'extraire l'URL depuis la réponse. Utilisation d'une URL par défaut.");
        return iiifServerUrl + "/iiif/3/" + System.currentTimeMillis() + ".jpg";
    }
}
