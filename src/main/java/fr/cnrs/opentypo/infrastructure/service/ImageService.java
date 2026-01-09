package fr.cnrs.opentypo.infrastructure.service;

import fr.cnrs.opentypo.domain.entity.Image;
import fr.cnrs.opentypo.infrastructure.persistence.ImageRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.file.UploadedFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service pour gérer l'upload et le stockage des images
 */
@Named
@ApplicationScoped
@Slf4j
public class ImageService {

    private static final String UPLOAD_DIR = "uploads/images";
    private static final String DEFAULT_IMAGE_URL = "/resources/img/o.png"; // Image par défaut

    @Inject
    private ImageRepository imageRepository;

    /**
     * Sauvegarde un fichier uploadé et crée une entité Image
     */
    public Image saveUploadedImage(UploadedFile uploadedFile) {
        if (uploadedFile == null || uploadedFile.getFileName() == null || uploadedFile.getFileName().isEmpty()) {
            return null;
        }

        try {
            // Créer le répertoire s'il n'existe pas
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Générer un nom de fichier unique
            String originalFileName = uploadedFile.getFileName();
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Sauvegarder le fichier
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(uploadedFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Créer l'entité Image avec l'URL relative
            Image image = new Image();
            image.setUrl("/" + UPLOAD_DIR + "/" + uniqueFileName);

            // Sauvegarder en base de données
            return imageRepository.save(image);

        } catch (IOException e) {
            log.error("Erreur lors de la sauvegarde de l'image", e);
            return null;
        }
    }

    /**
     * Retourne l'URL de l'image par défaut
     */
    public String getDefaultImageUrl() {
        return DEFAULT_IMAGE_URL;
    }

    /**
     * Retourne l'URL de l'image ou l'URL par défaut si l'image est null
     */
    public String getImageUrlOrDefault(Image image) {
        if (image != null && image.getUrl() != null && !image.getUrl().isEmpty()) {
            return image.getUrl();
        }
        return DEFAULT_IMAGE_URL;
    }
}
