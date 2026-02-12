package fr.cnrs.opentypo.presentation.bean.candidats.model;

import fr.cnrs.opentypo.domain.entity.Langue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Classe pour représenter une description de catégorie avec sa langue
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDescriptionItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private String valeur;
    private String langueCode;
    private Langue langue;
}
