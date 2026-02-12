package fr.cnrs.opentypo.presentation.bean.candidats.model;

import fr.cnrs.opentypo.domain.entity.Entity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandidatSauvegardeResult {
    private boolean success;
    private Entity savedEntity;
    private String errorMessage;
    private String successMessage;
}
