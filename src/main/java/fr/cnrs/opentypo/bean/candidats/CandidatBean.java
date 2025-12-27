package fr.cnrs.opentypo.bean.candidats;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Named("candidatBean")
@SessionScoped
@Getter
@Setter
public class CandidatBean implements Serializable {

    @Inject
    private fr.cnrs.opentypo.bean.UserBean userBean;

    private List<Candidat> candidats = new ArrayList<>();
    private Candidat candidatSelectionne;
    private Candidat nouveauCandidat;
    private int activeTabIndex = 0; // 0 = en cours, 1 = validés, 2 = refusés

    @PostConstruct
    public void init() {
        chargerCandidats();
    }

    public void chargerCandidats() {
        // Données d'exemple - à remplacer par un service réel
        if (candidats.isEmpty()) {
            candidats.add(new Candidat(1L, "TYPE-001", "Amphore type A", "fr", "Période romaine", 100, 200, 
                "Commentaire datation", "Amphore", "Description", "Production locale", "Atelier 1", 
                "Aire méditerranéenne", "Catégorie 1", "Céramique", "Ronde", "20x30cm", "Tournage", 
                "Fabrication manuelle", new ArrayList<>(), new ArrayList<>(), "REF-001", "TYP-001", 
                "ID-001", "V1", "Bibliographie", Candidat.Statut.EN_COURS, LocalDateTime.now(), null, "admin"));
            
            candidats.add(new Candidat(2L, "TYPE-002", "Vase type B", "fr", "Période grecque", 300, 400, 
                "Commentaire", "Vase", "Description", "Production", "Atelier 2", "Aire", "Catégorie", 
                "Céramique", "Oval", "15x25cm", "Moulage", "Fabrication", new ArrayList<>(), new ArrayList<>(), 
                "REF-002", "TYP-002", "ID-002", "V1", "Bibliographie", Candidat.Statut.VALIDE, 
                LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(1), "admin"));
        }
    }

    public List<Candidat> getCandidatsEnCours() {
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.EN_COURS)
            .collect(Collectors.toList());
    }

    public List<Candidat> getCandidatsValides() {
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.VALIDE)
            .collect(Collectors.toList());
    }

    public List<Candidat> getCandidatsRefuses() {
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.REFUSE)
            .collect(Collectors.toList());
    }

    public void initNouveauCandidat() {
        nouveauCandidat = new Candidat();
        nouveauCandidat.setCreateur(userBean.getUsername());
        nouveauCandidat.setStatut(Candidat.Statut.EN_COURS);
    }

    public void sauvegarderCandidat() {
        if (nouveauCandidat.getId() == null) {
            // Nouveau candidat
            Long nouveauId = candidats.stream()
                .mapToLong(Candidat::getId)
                .max()
                .orElse(0L) + 1;
            nouveauCandidat.setId(nouveauId);
            nouveauCandidat.setDateCreation(LocalDateTime.now());
            candidats.add(nouveauCandidat);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le candidat a été créé avec succès."));
        } else {
            // Modification
            nouveauCandidat.setDateModification(LocalDateTime.now());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le candidat a été modifié avec succès."));
        }
        
        PrimeFaces.current().ajax().update(":growl, :candidatsForm");
        PrimeFaces.current().executeScript("window.location.href='/candidats/candidats.xhtml';");
    }

    public void validerCandidat(Candidat candidat) {
        candidat.setStatut(Candidat.Statut.VALIDE);
        candidat.setDateModification(LocalDateTime.now());
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès",
                "Le candidat a été validé."));
        PrimeFaces.current().ajax().update(":growl, :candidatsForm");
    }

    public void refuserCandidat(Candidat candidat) {
        candidat.setStatut(Candidat.Statut.REFUSE);
        candidat.setDateModification(LocalDateTime.now());
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès",
                "Le candidat a été refusé."));
        PrimeFaces.current().ajax().update(":growl, :candidatsForm");
    }

    public void supprimerCandidat(Candidat candidat) {
        candidats.remove(candidat);
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Succès",
                "Le candidat a été supprimé."));
        PrimeFaces.current().ajax().update(":growl, :candidatsForm");
    }

    public String visualiserCandidat(Candidat candidat) {
        candidatSelectionne = candidat;
        return "/candidats/view.xhtml?faces-redirect=true";
    }
}

