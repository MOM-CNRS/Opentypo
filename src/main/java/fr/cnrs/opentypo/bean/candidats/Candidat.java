package fr.cnrs.opentypo.bean.candidats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Candidat implements Serializable {
    
    public enum Statut {
        EN_COURS,
        VALIDE,
        REFUSE
    }
    
    private Long id;
    private String typeCode;
    private String label;
    private String langue = "fr";
    private String periode;
    private Integer tpq;
    private Integer taq;
    private String commentaireDatation;
    private String appellationUsuelle;
    private String description;
    private String production;
    private String ateliers;
    private String aireCirculation;
    private String categorieFonctionnelle;
    private String materiaux;
    private String forme;
    private String dimensions;
    private String technique;
    private String fabrication;
    private List<String> attestations = new ArrayList<>();
    private List<String> sitesArcheologiques = new ArrayList<>();
    private String referentiel;
    private String typologiqueScientifique;
    private String identifiantPerenne;
    private String ancienneVersion;
    private String bibliographie;
    private Statut statut = Statut.EN_COURS;
    private LocalDateTime dateCreation = LocalDateTime.now();
    private LocalDateTime dateModification;
    private String createur;
}

