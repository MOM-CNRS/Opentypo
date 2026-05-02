package fr.cnrs.opentypo.application.import_typology;

/**
 * Colonnes CSV pour l'import sous une collection Monnaie (séparateur recommandé {@code ;}).
 * Les champs communs avec la céramique réutilisent les mêmes noms logiques.
 */
public final class TypologyImportMonnaieConstants {

    private TypologyImportMonnaieConstants() {
    }

    public static final String COL_DESCRIPTION_DROIT = "description_droit";
    public static final String COL_DESCRIPTION_LEGENDE_DROIT = "description_legende_droit";
    public static final String COL_DESCRIPTION_REVERS = "description_revers";
    public static final String COL_DESCRIPTION_LEGENDE_REVERS = "description_legende_revers";

    public static final String COL_CARACT_PHYS_MATERIAU = "caract_phys_materiau";
    public static final String COL_CARACT_PHYS_DENOMINATION = "caract_phys_denomination";
    public static final String COL_CARACT_PHYS_VALEUR = "caract_phys_valeur";
    public static final String COL_CARACT_PHYS_TECHNIQUE = "caract_phys_technique";

    /** Ordre des colonnes du modèle Monnaie (aligné produit / Excel FR). */
    public static final String[] KNOWN_COLUMNS_MONNAIE = new String[]{
            TypologyImportConstants.COL_CODE_CATEGORIE,
            TypologyImportConstants.COL_CODE_GROUPE,
            TypologyImportConstants.COL_CODE_SERIE,
            TypologyImportConstants.COL_CODE_TYPE,
            TypologyImportConstants.COL_NOM_COMPLET_FR,
            TypologyImportConstants.COL_NOM_COMPLET_EN,
            TypologyImportConstants.COL_APPELLATION_USUELLE,
            TypologyImportConstants.COL_DESCRIPTION_FR,
            TypologyImportConstants.COL_DESCRIPTION_EN,
            TypologyImportConstants.COL_AUTEUR_SCIENTIFIQUE,
            TypologyImportConstants.COL_ILLUSTRATIONS,
            TypologyImportConstants.COL_DATATION_PERIODE,
            TypologyImportConstants.COL_DATATION_TPQ,
            TypologyImportConstants.COL_DATATION_TAQ,
            TypologyImportConstants.COL_DATATION_COMMENTAIRE,
            TypologyImportConstants.COL_PRODUCTION_VALUE,
            TypologyImportConstants.COL_PRODUCTION_ATELIERS,
            TypologyImportConstants.COL_PRODUCTION_AIRE_CIRCULATION,
            TypologyImportConstants.COL_ATTESTATIONS_VALEUR,
            TypologyImportConstants.COL_ATTESTATIONS_CORPUS_LIE,
            COL_DESCRIPTION_DROIT,
            COL_DESCRIPTION_LEGENDE_DROIT,
            COL_DESCRIPTION_REVERS,
            COL_DESCRIPTION_LEGENDE_REVERS,
            COL_CARACT_PHYS_MATERIAU,
            COL_CARACT_PHYS_DENOMINATION,
            TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE,
            COL_CARACT_PHYS_VALEUR,
            COL_CARACT_PHYS_TECHNIQUE,
            TypologyImportConstants.COL_REFERENCES_REFERENTIEL,
            TypologyImportConstants.COL_REFERENCES_TYPOLOGIE_SCIENTIFIQUE,
            TypologyImportConstants.COL_RELATIONS_ALIGNEMENTS_INTERNE,
            TypologyImportConstants.COL_RELATIONS_ALIGNEMENTS_EXTERNE,
            TypologyImportConstants.COL_COMMENTAIRE
    };
}
