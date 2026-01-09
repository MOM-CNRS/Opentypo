package fr.cnrs.opentypo.common.constant;

/**
 * Constantes liées aux entités
 */
public final class EntityConstants {

    private EntityConstants() {
        // Classe utilitaire - pas d'instanciation
    }

    // Types d'entités
    public static final String ENTITY_TYPE_COLLECTION = "COLLECTION";
    public static final String ENTITY_TYPE_REFERENTIEL = "REFERENTIEL";
    public static final String ENTITY_TYPE_CATEGORY = "CATEGORY";
    public static final String ENTITY_TYPE_GROUP = "GROUP";
    public static final String ENTITY_TYPE_SERIES = "SERIES";
    public static final String ENTITY_TYPE_TYPE = "TYPE";

    // Longueurs maximales
    public static final int MAX_CODE_LENGTH = 100;
    public static final int MAX_LABEL_LENGTH = 255;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;

    // Messages d'erreur de validation
    public static final String ERROR_CODE_REQUIRED = "Le code est requis.";
    public static final String ERROR_LABEL_REQUIRED = "Le label est requis.";
    public static final String ERROR_CODE_TOO_LONG = "Le code ne peut pas dépasser " + MAX_CODE_LENGTH + " caractères.";
    public static final String ERROR_LABEL_TOO_LONG = "Le label ne peut pas dépasser " + MAX_LABEL_LENGTH + " caractères.";
    public static final String ERROR_CODE_ALREADY_EXISTS = "Un élément avec ce code existe déjà.";
}

