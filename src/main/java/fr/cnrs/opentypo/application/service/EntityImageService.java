package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.OpenTypoApplication;
import fr.cnrs.opentypo.domain.entity.Image;
import fr.cnrs.opentypo.infrastructure.persistence.ImageRepository;
import fr.cnrs.opentypo.presentation.bean.PartToMultipartFileAdapter;
import fr.cnrs.opentypo.presentation.bean.candidats.UploadedFileToMultipartFileAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service pour sauvegarder les images uploadées dans un répertoire "images"
 * à côté du JAR déployé. Retourne l'URL complète pour stockage en base.
 */
@Service
@Slf4j
public class EntityImageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    @Value("${opentypo.images.path:}")
    private String imagesPathOverride;

    @Autowired
    private ImageRepository imageRepository;

    /**
     * Sauvegarde un fichier image uploadé et retourne l'URL à stocker en base.
     * L'URL est relative au contexte (ex: /opentypo/uploaded-images/xxx.jpg).
     *
     * @param file        le fichier uploadé
     * @param contextPath le contexte de l'application (ex: "" ou "/opentypo")
     * @return l'URL complète à stocker, ou null en cas d'erreur
     */
    public String saveUploadedImage(MultipartFile file, String contextPath) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide ou null");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        if (!isAllowedImage(contentType, originalFilename)) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Utilisez JPG, PNG, GIF ou WebP.");
        }

        Path basePath = getImagesBasePath();
        Files.createDirectories(basePath);

        String extension = getExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        Path targetPath = basePath.resolve(uniqueFilename);
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Image sauvegardée: {}", targetPath.toAbsolutePath());

        String urlPath = (contextPath != null ? contextPath : "") + "/uploaded-images/" + uniqueFilename;
        return urlPath;
    }

    /**
     * Sauvegarde un fichier depuis jakarta.servlet.http.Part (h:inputFile).
     */
    public String saveUploadedImage(jakarta.servlet.http.Part part, String contextPath) throws IOException {
        if (part == null || part.getSize() == 0) {
            throw new IllegalArgumentException("Le fichier est vide ou null");
        }
        MultipartFile multipartFile = new PartToMultipartFileAdapter(part);
        return saveUploadedImage(multipartFile, contextPath);
    }

    /**
     * Sauvegarde un fichier depuis PrimeFaces UploadedFile.
     */
    public String saveUploadedImage(org.primefaces.model.file.UploadedFile file, String contextPath) throws IOException {
        if (file == null || file.getSize() == 0) {
            throw new IllegalArgumentException("Le fichier est vide ou null");
        }
        MultipartFile multipartFile = new UploadedFileToMultipartFileAdapter(file);
        return saveUploadedImage(multipartFile, contextPath);
    }

    private boolean isAllowedImage(String contentType, String filename) {
        // Priorité à l'extension : certains navigateurs/OS envoient application/octet-stream pour .jpeg
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith)) {
                return true;
            }
        }
        // Sinon vérifier le content-type (image/jpeg, image/png, etc.)
        if (contentType != null) {
            String ct = contentType.toLowerCase().split(";")[0].trim();
            return ALLOWED_CONTENT_TYPES.stream().anyMatch(ct::equals);
        }
        return false;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        String ext = filename.substring(filename.lastIndexOf('.'));
        return ALLOWED_EXTENSIONS.contains(ext.toLowerCase()) ? ext : ".jpg";
    }

    /**
     * Retourne le chemin du répertoire images.
     * Priorité : opentypo.images.path > répertoire du JAR/images > user.dir/images
     */
    public Path getImagesBasePath() {
        if (StringUtils.hasText(imagesPathOverride)) {
            return Paths.get(imagesPathOverride);
        }
        try {
            URI uri = OpenTypoApplication.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            Path codePath = Paths.get(uri);
            if (codePath.toString().endsWith(".jar")) {
                return codePath.getParent().resolve("images");
            }
            return codePath.resolve("images");
        } catch (Exception e) {
            log.warn("Impossible de déterminer l'emplacement du JAR, utilisation de user.dir: {}", e.getMessage());
        }
        return Paths.get(System.getProperty("user.dir", "."), "images");
    }

    /**
     * Supprime les fichiers physiques des images uploadées localement pour une entité.
     * Les URLs contenant "/uploaded-images/" correspondent à des fichiers locaux.
     * Les URLs externes (http://, https://) sont ignorées.
     *
     * @param entityId l'ID de l'entité
     */
    public void deletePhysicalFilesForEntity(Long entityId) {
        if (entityId == null) {
            return;
        }
        List<Image> images = imageRepository.findByEntity_Id(entityId);
        Path basePath = getImagesBasePath();
        for (Image image : images) {
            String url = image.getUrl();
            if (url == null || !url.contains("/uploaded-images/")) {
                continue;
            }
            String filename = url.substring(url.lastIndexOf("/") + 1);
            if (filename.isEmpty()) {
                continue;
            }
            Path filePath = basePath.resolve(filename);
            try {
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Fichier image supprimé: {}", filePath.toAbsolutePath());
                }
            } catch (IOException e) {
                log.warn("Impossible de supprimer le fichier image {}: {}", filePath, e.getMessage());
            }
        }
    }
}
