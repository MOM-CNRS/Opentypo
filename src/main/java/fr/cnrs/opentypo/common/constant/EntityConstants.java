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
    public static final String ENTITY_TYPE_REFERENCE = "REFERENTIEL";
    public static final String ENTITY_TYPE_CATEGORY = "CATEGORIE";
    public static final String ENTITY_TYPE_GROUP = "GROUPE";
    public static final String ENTITY_TYPE_SERIES = "SERIE";
    public static final String ENTITY_TYPE_TYPE = "TYPE";

    /** Longueur max des colonnes VARCHAR en base (voir migration V61). */
    public static final int VARCHAR_COLUMN_MAX_LENGTH = 1000;

    // Longueurs maximales (alignées sur VARCHAR_COLUMN_MAX_LENGTH)
    public static final int MAX_CODE_LENGTH = VARCHAR_COLUMN_MAX_LENGTH;
    public static final int MAX_LABEL_LENGTH = VARCHAR_COLUMN_MAX_LENGTH;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;

    // Messages d'erreur de validation
    public static final String ERROR_CODE_REQUIRED = "Le code est requis.";
    public static final String ERROR_LABEL_REQUIRED = "Le label est requis.";
    public static final String ERROR_CODE_TOO_LONG = "Le code ne peut pas dépasser " + MAX_CODE_LENGTH + " caractères.";
    public static final String ERROR_LABEL_TOO_LONG = "Le label ne peut pas dépasser " + MAX_LABEL_LENGTH + " caractères.";
    public static final String ERROR_CODE_ALREADY_EXISTS = "Un élément avec ce code existe déjà.";
    public static final String ERROR_CATEGORY_CODE_EXISTS_IN_REFERENCE =
            "Une catégorie avec ce code existe déjà dans ce référentiel.";
    public static final String ERROR_GROUP_CODE_EXISTS_IN_REFERENCE =
            "Un groupe avec ce code existe déjà dans ce référentiel.";
    public static final String ERROR_SERIE_CODE_EXISTS_IN_GROUP =
            "Une série avec ce code existe déjà dans ce groupe.";
    public static final String ERROR_TYPE_CODE_EXISTS_IN_GROUP =
            "Un type avec ce code existe déjà dans ce groupe.";
}

