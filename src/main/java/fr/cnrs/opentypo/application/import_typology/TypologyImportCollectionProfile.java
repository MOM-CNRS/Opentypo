package fr.cnrs.opentypo.application.import_typology;

/**
 * Collection du référentiel cible : détermine le schéma CSV (champs description / caractéristiques physiques).
 */
public enum TypologyImportCollectionProfile {
    CERAMIQUE,
    MONNAIE,
    INSTRUMENTUM,
    /** Collection non reconnue pour l’import CSV. */
    UNSUPPORTED
}
