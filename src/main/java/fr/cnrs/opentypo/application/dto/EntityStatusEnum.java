package fr.cnrs.opentypo.application.dto;

import lombok.Getter;

/**
 * Statuts des entités. La visibilité est déterminée uniquement par le statut :
 * - PROPOSITION : brouillon en cours de validation
 * - PUBLIQUE : validé, visible par tous
 * - PRIVEE : privé, visible uniquement par les gestionnaires
 * - REFUSE : refusé
 */
@Getter
public enum EntityStatusEnum {

    PROPOSITION,
    PUBLIQUE,
    PRIVEE,
    REFUSE

}