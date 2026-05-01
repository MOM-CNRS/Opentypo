package fr.cnrs.opentypo.application.import_typology;

/**
 * Nature de la ligne d'import (règles métier catégorie → groupe → série → type).
 */
public enum TypologyImportKind {
    CATEGORIE,
    GROUPE,
    SERIE,
    TYPE_SOUS_SERIE,
    TYPE_SOUS_GROUPE,
    /** Ligne non classifiable (erreurs de saisie). */
    NON_CLASSIFIE
}
