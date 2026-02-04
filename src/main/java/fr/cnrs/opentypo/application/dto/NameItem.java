package fr.cnrs.opentypo.application.dto;

import fr.cnrs.opentypo.domain.entity.Langue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;


/**
 * Classe interne pour gérer les noms multilingues
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NameItem implements Serializable {

    private String nom;
    private String langueCode;
    private Langue langue;
}