package fr.cnrs.opentypo.application.dto;

import fr.cnrs.opentypo.domain.entity.Langue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;


/**
 * Classe interne pour gérer les descriptions multilingues
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DescriptionItem implements Serializable {

    private String valeur;
    private String langueCode;
    private Langue langue;
}