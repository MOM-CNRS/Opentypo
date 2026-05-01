package fr.cnrs.opentypo.application.import_typology;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Colonnes du fichier CSV d'import typologique (v1 : jeu aligné sur les métadonnées utilisées pour la collection Céramique).
 * Les autres typologies (Monnaie, Instrumentum, etc.) pourront étendre ou compléter ces colonnes ultérieurement.
 */
public final class TypologyImportConstants {

    private TypologyImportConstants() {
    }

    /** Séparateur pour listes multi-valeurs (images, alignements texte, etc.). */
    public static final String LIST_SEPARATOR = "\\|\\|";

    public static final String COL_CODE_CATEGORIE = "code_categorie";
    public static final String COL_CODE_GROUPE = "code_groupe";
    public static final String COL_CODE_SERIE = "code_serie";
    public static final String COL_CODE_TYPE = "code_type";

    // -------------------------------------------
    // Modèle Céramique (noms fournis par métier)
    // -------------------------------------------
    public static final String COL_NOM_COMPLET_FR = "nom_complet_fr";
    public static final String COL_NOM_COMPLET_EN = "nom_complet_en";
    public static final String COL_APPELLATION_USUELLE = "appellation_usuelle";
    public static final String COL_DESCRIPTION_FR = "description_fr";
    public static final String COL_DESCRIPTION_EN = "description_en";
    public static final String COL_ILLUSTRATIONS = "illustrations";
    public static final String COL_DATATION_PERIODE = "datation_periode";
    public static final String COL_DATATION_TPQ = "datation_tpq";
    public static final String COL_DATATION_TAQ = "datation_taq";
    public static final String COL_DATATION_COMMENTAIRE = "datation_commentaire";
    public static final String COL_PRODUCTION_VALUE = "production_value";
    public static final String COL_PRODUCTION_ATELIERS = "production_ateliers";
    public static final String COL_PRODUCTION_AIRE_CIRCULATION = "production_aire_circulation";
    public static final String COL_ATTESTATIONS_VALEUR = "attestations_valeur";
    public static final String COL_ATTESTATIONS_CORPUS_LIE = "attestations_corpus_lie";
    public static final String COL_DESCRIPTION_FORM = "description_form";
    public static final String COL_DESCRIPTION_DECORS = "description_decors";
    public static final String COL_DESCRIPTION_MARQUES = "description_marques";
    public static final String COL_DESCRIPTION_FONCTION = "description_fonction";
    public static final String COL_CARACT_PHYS_METROLOGIE = "caract_phys_metrologie";
    public static final String COL_CARACT_PHYS_FABRICATION = "caract_phys_fabrication";
    public static final String COL_CARACT_PHYS_DESCRIPTION_PATE = "caract_phys_description_pate";
    public static final String COL_CARACT_PHYS_COULEUR_PATE = "caract_phys_couleur_pate";
    public static final String COL_CARACT_PHYS_NATURE_PATE = "caract_phys_nature_pate";
    public static final String COL_CARACT_PHYS_INCLUSION = "caract_phys_inclusion";
    public static final String COL_CARACT_PHYS_CUISSON = "caract_phys_cuisson";
    public static final String COL_REFERENCES_REFERENTIEL = "references_referentiel";
    public static final String COL_REFERENCES_TYPOLOGIE_SCIENTIFIQUE = "references_typologie_scientifique";
    public static final String COL_RELATIONS_ALIGNEMENTS_INTERNE = "relations_alignements_interne";
    public static final String COL_RELATIONS_ALIGNEMENTS_EXTERNE = "relations_alignements_externe";
    public static final String COL_AUTEURS_SCIENTIFIQUES = "auteurs_scientifiques";
    public static final String COL_COMMENTAIRE = "commentaire";

    /**
     * Contenu minimal : uniquement la ligne d'en-tête (titres de colonnes), UTF-8.
     */
    public static String csvTemplateHeaderOnly() {
        return String.join(",", KNOWN_COLUMNS) + "\n";
    }

    /**
     * Fichier modèle téléchargeable : en-tête + une ligne d'exemple complète.
     * {@link TypologyImportCollectionProfile#CERAMIQUE} → {@code import/typology-import-template-ceramique.csv} ;
     * {@link TypologyImportCollectionProfile#MONNAIE} → {@code import/typology-import-template-monnaie.csv} ;
     * {@link TypologyImportCollectionProfile#INSTRUMENTUM} → {@code import/typology-import-template-instrumentum.csv}.
     */
    public static String csvTemplateHeaderAndExample(TypologyImportCollectionProfile profile) {
        String resource = switch (profile) {
            case MONNAIE -> "import/typology-import-template-monnaie.csv";
            case INSTRUMENTUM -> "import/typology-import-template-instrumentum.csv";
            case CERAMIQUE, UNSUPPORTED -> "import/typology-import-template-ceramique.csv";
        };
        try (InputStream in = TypologyImportConstants.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                return switch (profile) {
                    case MONNAIE -> String.join(";", TypologyImportMonnaieConstants.KNOWN_COLUMNS_MONNAIE) + "\n";
                    case INSTRUMENTUM -> String.join(";", TypologyImportInstrumentumConstants.KNOWN_COLUMNS_INSTRUMENTUM) + "\n";
                    case CERAMIQUE, UNSUPPORTED -> csvTemplateHeaderOnly();
                };
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return switch (profile) {
                case MONNAIE -> String.join(";", TypologyImportMonnaieConstants.KNOWN_COLUMNS_MONNAIE) + "\n";
                case INSTRUMENTUM -> String.join(";", TypologyImportInstrumentumConstants.KNOWN_COLUMNS_INSTRUMENTUM) + "\n";
                case CERAMIQUE, UNSUPPORTED -> csvTemplateHeaderOnly();
            };
        }
    }

    /** Noms d'en-tête normalisés (minuscules) pour le parseur. */
    public static final String[] KNOWN_COLUMNS = new String[]{
            COL_CODE_CATEGORIE,
            COL_CODE_GROUPE,
            COL_CODE_SERIE,
            COL_CODE_TYPE,
            COL_NOM_COMPLET_FR,
            COL_NOM_COMPLET_EN,
            COL_APPELLATION_USUELLE,
            COL_DESCRIPTION_FR,
            COL_DESCRIPTION_EN,
            COL_ILLUSTRATIONS,
            COL_DATATION_PERIODE,
            COL_DATATION_TPQ,
            COL_DATATION_TAQ,
            COL_DATATION_COMMENTAIRE,
            COL_PRODUCTION_VALUE,
            COL_PRODUCTION_ATELIERS,
            COL_PRODUCTION_AIRE_CIRCULATION,
            COL_ATTESTATIONS_VALEUR,
            COL_ATTESTATIONS_CORPUS_LIE,
            COL_DESCRIPTION_FORM,
            COL_DESCRIPTION_DECORS,
            COL_DESCRIPTION_MARQUES,
            COL_DESCRIPTION_FONCTION,
            COL_CARACT_PHYS_METROLOGIE,
            COL_CARACT_PHYS_FABRICATION,
            COL_CARACT_PHYS_DESCRIPTION_PATE,
            COL_CARACT_PHYS_COULEUR_PATE,
            COL_CARACT_PHYS_NATURE_PATE,
            COL_CARACT_PHYS_INCLUSION,
            COL_CARACT_PHYS_CUISSON,
            COL_REFERENCES_REFERENTIEL,
            COL_REFERENCES_TYPOLOGIE_SCIENTIFIQUE,
            COL_RELATIONS_ALIGNEMENTS_INTERNE,
            COL_RELATIONS_ALIGNEMENTS_EXTERNE,
            COL_AUTEURS_SCIENTIFIQUES,
            COL_COMMENTAIRE
    };
}
