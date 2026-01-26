package fr.cnrs.opentypo.presentation.bean.candidats.model;

import fr.cnrs.opentypo.domain.entity.Langue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Classe pour représenter un label de catégorie avec sa langue
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryLabelItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nom;
    private String langueCode;
    private Langue langue;
}
