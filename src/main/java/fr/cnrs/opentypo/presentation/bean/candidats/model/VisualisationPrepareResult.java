package fr.cnrs.opentypo.presentation.bean.candidats.model;

import fr.cnrs.opentypo.domain.entity.Entity;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Résultat de la préparation de la visualisation d'un candidat. */
@Data
@Builder
public class VisualisationPrepareResult {
    private boolean success;
    private String errorMessage;
    private String redirectUrl;
    private Long selectedEntityTypeId;
    private String entityCode;
    private String selectedLangueCode;
    private String entityLabel;
    private Long selectedCollectionId;
    private Entity selectedParentEntity;
    private Entity currentEntity;
    @Builder.Default
    private List<CategoryLabelItem> candidatLabels = new ArrayList<>();
    @Builder.Default
    private List<CategoryDescriptionItem> descriptions = new ArrayList<>();
    private String periode;
    private Step3FormData step3Data;
}
