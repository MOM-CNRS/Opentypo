package fr.cnrs.opentypo.application.dto;

import lombok.Getter;

/**
 * Statuts des entités. La visibilité est déterminée uniquement par le statut :
 * - PROPOSITION : brouillon en cours de rédaction
 * - IN_VALIDATION : demande de validation envoyée, en attente du valideur
 * - PUBLIQUE : validé, visible par tous
 * - PRIVEE : privé, visible uniquement par les gestionnaires
 * - REFUSE : refusé
 */
@Getter
public enum EntityStatusEnum {

    PROPOSITION,
    IN_VALIDATION,
    PUBLIQUE,
    PRIVEE,
    REFUSE

}