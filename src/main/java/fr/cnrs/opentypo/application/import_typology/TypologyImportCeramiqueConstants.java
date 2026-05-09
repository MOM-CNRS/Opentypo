package fr.cnrs.opentypo.application.import_typology;

/**
 * Colonnes CSV specifiques au profil Ceramique.
 * Ce fichier complete les colonnes communes definies dans {@link TypologyImportConstants}.
 */
public final class TypologyImportCeramiqueConstants {

    private TypologyImportCeramiqueConstants() {
    }

    public static final String COL_DESCRIPTION_FORM = "description_form";
    public static final String COL_DESCRIPTION_DECORS = "description_decors";
    public static final String COL_DESCRIPTION_MARQUES = "description_marques";
    public static final String COL_DESCRIPTION_FONCTION = "description_fonction";

    public static final String COL_CARACT_PHYS_DESCRIPTION_PATE = "caract_phys_description_pate";
    public static final String COL_CARACT_PHYS_COULEUR_PATE = "caract_phys_couleur_pate";
    public static final String COL_CARACT_PHYS_NATURE_PATE = "caract_phys_nature_pate";
    public static final String COL_CARACT_PHYS_INCLUSION = "caract_phys_inclusion";
    public static final String COL_CARACT_PHYS_CUISSON = "caract_phys_cuisson";

    /** Ordre des colonnes du modele Ceramique (commun + specifiques ceramique). */
    public static final String[] KNOWN_COLUMNS_CERAMIQUE = new String[]{
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
            COL_DESCRIPTION_FORM,
            COL_DESCRIPTION_DECORS,
            COL_DESCRIPTION_MARQUES,
            COL_DESCRIPTION_FONCTION,
            TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE,
            TypologyImportConstants.COL_CARACT_PHYS_FABRICATION,
            COL_CARACT_PHYS_DESCRIPTION_PATE,
            COL_CARACT_PHYS_COULEUR_PATE,
            COL_CARACT_PHYS_NATURE_PATE,
            COL_CARACT_PHYS_INCLUSION,
            COL_CARACT_PHYS_CUISSON,
            TypologyImportConstants.COL_REFERENCES_REFERENTIEL,
            TypologyImportConstants.COL_REFERENCES_TYPOLOGIE_SCIENTIFIQUE,
            TypologyImportConstants.COL_RELATIONS_ALIGNEMENTS_INTERNE,
            TypologyImportConstants.COL_RELATIONS_ALIGNEMENTS_EXTERNE,
            TypologyImportConstants.COL_COMMENTAIRE
    };
}
