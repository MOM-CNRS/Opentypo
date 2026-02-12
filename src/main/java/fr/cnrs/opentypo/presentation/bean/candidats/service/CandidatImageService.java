package fr.cnrs.opentypo.presentation.bean.candidats.service;

import fr.cnrs.opentypo.application.service.IiifImageService;
import fr.cnrs.opentypo.presentation.bean.candidats.UploadedFileToMultipartFileAdapter;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.file.UploadedFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service pour l'upload d'images dans le formulaire candidat.
 */
@Service
@Slf4j
public class CandidatImageService {

    @Inject private IiifImageService iiifImageService;

    public record UploadResult(boolean success, String imageUrl, String errorMessage) {}

    public UploadResult uploadImage(UploadedFile uploadedFile) {
        if (uploadedFile == null || uploadedFile.getSize() == 0) {
            return new UploadResult(false, null, "Aucun fichier sélectionné.");
        }
        try {
            MultipartFile multipartFile = new UploadedFileToMultipartFileAdapter(uploadedFile);
            String url = iiifImageService.uploadImage(multipartFile);
            log.info("Image uploadée avec succès. URL IIIF: {}", url);
            return new UploadResult(true, url, null);
        } catch (Exception e) {
            log.error("Erreur lors de l'upload de l'image vers IIIF", e);
            return new UploadResult(false, null, e.getMessage() != null ? e.getMessage() : "Erreur d'upload");
        }
    }
}
