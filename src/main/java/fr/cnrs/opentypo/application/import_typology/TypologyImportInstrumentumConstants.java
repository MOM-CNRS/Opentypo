package fr.cnrs.opentypo.application.import_typology;

/**
 * Colonnes CSV pour l'import sous une collection Instrumentum (séparateur recommandé {@code ;}).
 * Le fichier métier sépare explicitement {@code caract_phys_forme} et {@code caract_phys_dimensions}
 * (et non une colonne unique concaténée).
 */
public final class TypologyImportInstrumentumConstants {

    private TypologyImportInstrumentumConstants() {
    }

    /** Code stocké sur {@link fr.cnrs.opentypo.domain.entity.ReferenceOpentheso} pour la relation d'imitation. */
    public static final String OPENTHESO_CODE_RELATION_IMITATION = "RELATION_IMITATION";

    public static final String COL_DESCRIPTION_CATEGORIE_FONCTIONNELLE = "description_categorie_fonctionnelle";
    public static final String COL_DESCRIPTION_RELATION_IMITATION = "description_relation_imitation";
    public static final String COL_DESCRIPTION_DENOMINATION = "description_denomination";

    public static final String COL_CARACT_PHYS_MATERIAUX = "caract_phys_materiaux";
    public static final String COL_CARACT_PHYS_FORME = "caract_phys_forme";
    public static final String COL_CARACT_PHYS_DIMENSIONS = "caract_phys_dimensions";
    public static final String COL_CARACT_PHYS_TECHNIQUE = "caract_phys_technique";

    public static final String[] KNOWN_COLUMNS_INSTRUMENTUM = new String[]{
            TypologyImportConstants.COL_CODE_CATEGORIE,
            TypologyImportConstants.COL_CODE_GROUPE,
            TypologyImportConstants.COL_CODE_SERIE,
            TypologyImportConstants.COL_CODE_TYPE,
            TypologyImportConstants.COL_NOM_COMPLET_FR,
            TypologyImportConstants.COL_NOM_COMPLET_EN,
            TypologyImportConstants.COL_APPELLATION_USUELLE,
            TypologyImportConstants.COL_DESCRIPTION_FR,
            TypologyImportConstants.COL_DESCRIPTION_EN,
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
            TypologyImportConstants.COL_DESCRIPTION_DECORS,
            TypologyImportConstants.COL_DESCRIPTION_MARQUES,
            COL_DESCRIPTION_CATEGORIE_FONCTIONNELLE,
            COL_DESCRIPTION_RELATION_IMITATION,
            COL_DESCRIPTION_DENOMINATION,
            COL_CARACT_PHYS_MATERIAUX,
            COL_CARACT_PHYS_FORME,
            COL_CARACT_PHYS_DIMENSIONS,
            COL_CARACT_PHYS_TECHNIQUE,
            TypologyImportConstants.COL_CARACT_PHYS_FABRICATION,
            TypologyImportConstants.COL_REFERENCES_REFERENTIEL,
            TypologyImportConstants.COL_REFERENCES_TYPOLOGIE_SCIENTIFIQUE,
            TypologyImportConstants.COL_RELATIONS_ALIGNEMENTS_INTERNE,
            TypologyImportConstants.COL_RELATIONS_ALIGNEMENTS_EXTERNE,
            TypologyImportConstants.COL_AUTEURS_SCIENTIFIQUES,
            TypologyImportConstants.COL_COMMENTAIRE
    };
}
