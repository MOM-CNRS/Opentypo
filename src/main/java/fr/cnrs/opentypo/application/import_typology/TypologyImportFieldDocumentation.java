package fr.cnrs.opentypo.application.import_typology;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Libellés d’aide pour les colonnes CSV d’import typologique, selon la collection du référentiel.
 */
public final class TypologyImportFieldDocumentation {

    private TypologyImportFieldDocumentation() {
    }

    /**
     * Ligne affichable dans l’UI (nom technique + description).
     */
    public static final class FieldDocRow implements Serializable {
        private final String columnName;
        private final String description;

        public FieldDocRow(String columnName, String description) {
            this.columnName = columnName;
            this.description = description;
        }

        public String getColumnName() {
            return columnName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Colonnes dans l’ordre du modèle téléchargeable, avec la description adaptée au profil.
     */
    public static List<FieldDocRow> rowsForProfile(TypologyImportCollectionProfile profile) {
        TypologyImportCollectionProfile p = profile == TypologyImportCollectionProfile.UNSUPPORTED
                ? TypologyImportCollectionProfile.CERAMIQUE
                : profile;
        String[] order = switch (p) {
            case MONNAIE -> TypologyImportMonnaieConstants.KNOWN_COLUMNS_MONNAIE;
            case INSTRUMENTUM -> TypologyImportInstrumentumConstants.KNOWN_COLUMNS_INSTRUMENTUM;
            case CERAMIQUE, UNSUPPORTED -> TypologyImportConstants.KNOWN_COLUMNS;
        };
        Map<String, String> desc = descriptionsByColumn();
        List<FieldDocRow> rows = new ArrayList<>(order.length);
        for (String col : order) {
            rows.add(new FieldDocRow(col, desc.getOrDefault(col,
                    "Champ métier ; voir le modèle CSV et la documentation OpenTypo.")));
        }
        return rows;
    }

    private static Map<String, String> descriptionsByColumn() {
        Map<String, String> m = new LinkedHashMap<>();

        // Hiérarchie & identification
        m.put(TypologyImportConstants.COL_CODE_CATEGORIE,
                "Code technique de la catégorie — obligatoire sur chaque ligne ; ancrage dans la hiérarchie du référentiel.");
        m.put(TypologyImportConstants.COL_CODE_GROUPE,
                "Code du groupe : requis pour créer ou rattacher un groupe, une série ou un type sous ce groupe.");
        m.put(TypologyImportConstants.COL_CODE_SERIE,
                "Code de la série : pour une ligne « série » ou « type rattaché à une série ».");
        m.put(TypologyImportConstants.COL_CODE_TYPE,
                "Code du type : lorsque la ligne décrit un type (avec groupe, et série si besoin).");
        m.put(TypologyImportConstants.COL_NOM_COMPLET_FR,
                "Libellé principal en français (nom affiché du concept).");
        m.put(TypologyImportConstants.COL_NOM_COMPLET_EN,
                "Libellé en anglais (langue secondaire), si renseigné.");
        m.put(TypologyImportConstants.COL_APPELLATION_USUELLE,
                "Appellation(s) usuelle(s) : plusieurs entrées séparées par || ; même format que l’aire de circulation — libellé:url OpenTheso (url obligatoire pour un lien), ou texte seul.");

        // Descriptions & auteurs
        m.put(TypologyImportConstants.COL_DESCRIPTION_FR,
                "Description détaillée en français.");
        m.put(TypologyImportConstants.COL_DESCRIPTION_EN,
                "Description détaillée en anglais.");
        m.put(TypologyImportConstants.COL_AUTEUR_SCIENTIFIQUE,
                "Auteurs scientifiques liés au type : plusieurs valeurs séparées par || ; format recommandé {Prénom, Nom} pour chaque auteur. "
                        + "Format historique Nom:Prénom encore accepté. Si l’auteur n’existe pas en base, il est créé puis associé. "
                        + "En CSV séparé par des virgules, entourer la cellule de guillemets si elle contient des virgules.");

        // Illustrations & datation
        m.put(TypologyImportConstants.COL_ILLUSTRATIONS,
                "Illustrations : une ou plusieurs entrées séparées par || ; chaque entrée peut être une URL seule ou légende:url "
                        + "(deux-points avant http/https). Les URLs invalides sont ignorées à l’import.");
        m.put(TypologyImportConstants.COL_DATATION_PERIODE,
                "Période OpenTheso au format libellé:url (URL du concept dans OpenTheso).");
        m.put(TypologyImportConstants.COL_DATATION_TPQ,
                "Terminus post quem (année entière, optionnel).");
        m.put(TypologyImportConstants.COL_DATATION_TAQ,
                "Terminus ante quem (année entière, optionnel).");
        m.put(TypologyImportConstants.COL_DATATION_COMMENTAIRE,
                "Commentaire libre sur la datation.");

        // Production & attestations
        m.put(TypologyImportConstants.COL_PRODUCTION_VALUE,
                "Lieu ou mode de production OpenTheso : libellé:url.");
        m.put(TypologyImportConstants.COL_PRODUCTION_ATELIERS,
                "Ateliers ou lieux de production : liste ; entrées séparées par || ou selon les « : » définis dans le mapping import.");
        m.put(TypologyImportConstants.COL_PRODUCTION_AIRE_CIRCULATION,
                "Aires de circulation : plusieurs entrées || ; chaque entrée au format libellé:url OpenTheso.");
        m.put(TypologyImportConstants.COL_ATTESTATIONS_VALEUR,
                "Attestations / contextes : liste (séparateur || ou règles de liste du formulaire).");
        m.put(TypologyImportConstants.COL_ATTESTATIONS_CORPUS_LIE,
                "Corpus liés : texte libre ou liste selon le modèle.");

        // Céramique — description détail & pâte
        m.put(TypologyImportConstants.COL_DESCRIPTION_FORM,
                "Forme (référence OpenTheso) : libellé:url.");
        m.put(TypologyImportConstants.COL_DESCRIPTION_DECORS,
                "Décors : texte ou liste selon le domaine.");
        m.put(TypologyImportConstants.COL_DESCRIPTION_MARQUES,
                "Marques : valeurs séparées par || puis converties pour stockage.");
        m.put(TypologyImportConstants.COL_DESCRIPTION_FONCTION,
                "Fonction / usage OpenTheso : libellé:url.");
        m.put(TypologyImportConstants.COL_CARACT_PHYS_METROLOGIE,
                "Métrologie (céramique ou monnaie selon profil) : libellé:url lorsque stocké comme référence.");
        m.put(TypologyImportConstants.COL_CARACT_PHYS_FABRICATION,
                "Fabrication / façonnage OpenTheso : libellé:url.");
        m.put(TypologyImportConstants.COL_CARACT_PHYS_DESCRIPTION_PATE,
                "Description textuelle de la pâte (céramique).");
        m.put(TypologyImportConstants.COL_CARACT_PHYS_COULEUR_PATE,
                "Couleur de pâte OpenTheso : libellé:url.");
        m.put(TypologyImportConstants.COL_CARACT_PHYS_NATURE_PATE,
                "Nature de pâte OpenTheso : libellé:url.");
        m.put(TypologyImportConstants.COL_CARACT_PHYS_INCLUSION,
                "Inclusions OpenTheso : libellé:url.");
        m.put(TypologyImportConstants.COL_CARACT_PHYS_CUISSON,
                "Cuisson OpenTheso : libellé:url.");

        // Monnaie — descriptions & caractéristiques
        m.put(TypologyImportMonnaieConstants.COL_DESCRIPTION_DROIT,
                "Description du droit de la monnaie (texte libre).");
        m.put(TypologyImportMonnaieConstants.COL_DESCRIPTION_LEGENDE_DROIT,
                "Légende au droit.");
        m.put(TypologyImportMonnaieConstants.COL_DESCRIPTION_REVERS,
                "Description du revers.");
        m.put(TypologyImportMonnaieConstants.COL_DESCRIPTION_LEGENDE_REVERS,
                "Légende au revers.");
        m.put(TypologyImportMonnaieConstants.COL_CARACT_PHYS_MATERIAU,
                "Matériau OpenTheso : libellé:url.");
        m.put(TypologyImportMonnaieConstants.COL_CARACT_PHYS_DENOMINATION,
                "Dénomination OpenTheso : libellé:url.");
        m.put(TypologyImportMonnaieConstants.COL_CARACT_PHYS_VALEUR,
                "Valeur faciale / type OpenTheso : libellé:url.");
        m.put(TypologyImportMonnaieConstants.COL_CARACT_PHYS_TECHNIQUE,
                "Technique de frappe OpenTheso : libellé:url.");

        // Instrumentum — champs dédiés
        m.put(TypologyImportInstrumentumConstants.COL_DESCRIPTION_CATEGORIE_FONCTIONNELLE,
                "Catégorie fonctionnelle OpenTheso : libellé:url.");
        m.put(TypologyImportInstrumentumConstants.COL_DESCRIPTION_RELATION_IMITATION,
                "Relation d’imitation OpenTheso : libellé:url.");
        m.put(TypologyImportInstrumentumConstants.COL_DESCRIPTION_DENOMINATION,
                "Dénomination de l’objet OpenTheso : libellé:url.");
        m.put(TypologyImportInstrumentumConstants.COL_CARACT_PHYS_MATERIAUX,
                "Matériaux OpenTheso : libellé:url.");
        m.put(TypologyImportInstrumentumConstants.COL_CARACT_PHYS_FORME,
                "Forme OpenTheso : libellé:url.");
        m.put(TypologyImportInstrumentumConstants.COL_CARACT_PHYS_DIMENSIONS,
                "Dimensions OpenTheso : libellé:url.");
        m.put(TypologyImportInstrumentumConstants.COL_CARACT_PHYS_TECHNIQUE,
                "Technique de fabrication OpenTheso : libellé:url.");

        // Références & relations
        m.put(TypologyImportConstants.COL_REFERENCES_REFERENTIEL,
                "Références bibliographiques : entrées séparées par || ; format avec paires selon les règles import (références).");
        m.put(TypologyImportConstants.COL_REFERENCES_TYPOLOGIE_SCIENTIFIQUE,
                "Référence ou libellé de typologie scientifique (texte métadonnées).");
        m.put(TypologyImportConstants.COL_RELATIONS_ALIGNEMENTS_INTERNE,
                "Alignements internes (texte ou URIs selon saisie métier).");
        m.put(TypologyImportConstants.COL_RELATIONS_ALIGNEMENTS_EXTERNE,
                "Alignements externes (ex. Wikidata) : formats avec paires selon le mapping import.");

        m.put(TypologyImportConstants.COL_COMMENTAIRE,
                "Commentaire libre sur l’entité importée.");

        return m;
    }
}
