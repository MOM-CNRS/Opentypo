package fr.cnrs.opentypo.presentation.bean.candidats;

import fr.cnrs.opentypo.application.dto.EntityStatusEnum;
import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import fr.cnrs.opentypo.infrastructure.persistence.EntityRepository;
import fr.cnrs.opentypo.presentation.bean.LoginBean;
import fr.cnrs.opentypo.presentation.bean.candidats.converter.CandidatConverter;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean pour la gestion de la liste des candidats
 * Responsable du chargement, filtrage, validation et suppression des candidats
 */
@Named("candidatListBean")
@SessionScoped
@Getter
@Setter
@Slf4j
public class CandidatListBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private EntityRepository entityRepository;

    @Inject
    private LoginBean loginBean;

    @Inject
    private CandidatConverter candidatConverter;

    private List<Candidat> candidats = new ArrayList<>();
    private Candidat candidatSelectionne;
    private Candidat candidatAValider;
    private Candidat candidatASupprimer;
    private int activeTabIndex = 0; // 0 = en cours, 1 = validés, 2 = refusés
    private boolean candidatsLoaded = false;

    @PostConstruct
    public void init() {
        chargerCandidats();
    }

    /**
     * Charge la page des candidats et vérifie les paramètres de requête pour les messages
     */
    public void loadCandidatsPage() {
        log.debug("Chargement de la page candidats, rechargement des données");
        candidatsLoaded = false;
        chargerCandidats();
        
        // Vérifier les paramètres de requête pour afficher des messages
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String success = facesContext.getExternalContext().getRequestParameterMap().get("success");
            String error = facesContext.getExternalContext().getRequestParameterMap().get("error");
            
            if ("true".equals(success)) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Succès",
                        "Le candidat a été créé avec succès. Vous pouvez le voir dans la liste des candidats en cours."));
                PrimeFaces.current().ajax().update(":growl");
            } else if ("true".equals(error)) {
                facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Une erreur est survenue lors de la sauvegarde."));
                PrimeFaces.current().ajax().update(":growl");
            }
        }
    }

    /**
     * Charge tous les candidats depuis la base de données
     */
    public void chargerCandidats() {
        try {
            List<Entity> entitiesProposition = entityRepository.findByStatut(EntityStatusEnum.PROPOSITION.name());
            List<Entity> entitiesAccepted = entityRepository.findByStatut(EntityStatusEnum.ACCEPTED.name());
            List<Entity> entitiesRefused = entityRepository.findByStatut(EntityStatusEnum.REFUSED.name());
            
            candidats.clear();
            candidats.addAll(entitiesProposition.stream()
                .map(candidatConverter::convertEntityToCandidat)
                .collect(Collectors.toList()));
            candidats.addAll(entitiesAccepted.stream()
                .map(candidatConverter::convertEntityToCandidat)
                .collect(Collectors.toList()));
            candidats.addAll(entitiesRefused.stream()
                .map(candidatConverter::convertEntityToCandidat)
                .collect(Collectors.toList()));
            
            candidatsLoaded = true;
            log.info("Chargement des candidats terminé: {} PROPOSITION, {} ACCEPTED, {} REFUSED", 
                entitiesProposition.size(), entitiesAccepted.size(), entitiesRefused.size());
        } catch (Exception e) {
            log.error("Erreur lors du chargement des candidats depuis la base de données", e);
            candidats.clear();
            candidatsLoaded = false;
        }
    }

    /**
     * Retourne la liste des candidats en cours (statut PROPOSITION)
     */
    public List<Candidat> getCandidatsEnCours() {
        chargerCandidatsIfNeeded();
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.EN_COURS)
            .collect(Collectors.toList());
    }

    /**
     * Retourne la liste des candidats validés (statut ACCEPTED)
     */
    public List<Candidat> getCandidatsValides() {
        chargerCandidatsIfNeeded();
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.VALIDE)
            .collect(Collectors.toList());
    }

    /**
     * Retourne la liste des candidats refusés (statut REFUSED)
     */
    public List<Candidat> getCandidatsRefuses() {
        chargerCandidatsIfNeeded();
        return candidats.stream()
            .filter(c -> c.getStatut() == Candidat.Statut.REFUSE)
            .collect(Collectors.toList());
    }

    /**
     * Recharge les candidats si nécessaire
     */
    private void chargerCandidatsIfNeeded() {
        if (!candidatsLoaded) {
            chargerCandidats();
        }
    }

    /**
     * Prépare la validation d'un candidat
     */
    public void prepareValidateCandidat(Candidat candidat) {
        this.candidatAValider = candidat;
    }

    /**
     * Prépare la suppression d'un candidat
     */
    public void prepareDeleteCandidat(Candidat candidat) {
        this.candidatASupprimer = candidat;
    }

    /**
     * Valide un candidat (change le statut à ACCEPTED)
     */
    public void validerCandidat(Candidat candidat) {
        try {
            if (candidat == null || candidat.getId() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Candidat invalide."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            Entity entity = entityRepository.findById(candidat.getId()).orElse(null);
            if (entity == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Entité introuvable."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            entity.setStatut(EntityStatusEnum.ACCEPTED.name());
            entityRepository.save(entity);
            
            candidatsLoaded = false;
            chargerCandidats();
            
            Utilisateur currentUser = loginBean.getCurrentUser();
            String userName = currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "Utilisateur";
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le candidat a été validé par " + userName + "."));
            PrimeFaces.current().ajax().update(":growl, :candidatsForm");
        } catch (Exception e) {
            log.error("Erreur lors de la validation du candidat", e);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Une erreur est survenue lors de la validation : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl");
        }
    }

    /**
     * Refuse un candidat (change le statut à REFUSED)
     */
    public void refuserCandidat(Candidat candidat) {
        try {
            if (candidat == null || candidat.getId() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Candidat invalide."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            Entity entity = entityRepository.findById(candidat.getId()).orElse(null);
            if (entity == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Entité introuvable."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            entity.setStatut(EntityStatusEnum.REFUSED.name());
            entityRepository.save(entity);
            
            candidatsLoaded = false;
            chargerCandidats();
            
            Utilisateur currentUser = loginBean.getCurrentUser();
            String userName = currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "Utilisateur";
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le candidat a été refusé par " + userName + "."));
            PrimeFaces.current().ajax().update(":growl, :candidatsForm");
        } catch (Exception e) {
            log.error("Erreur lors du refus du candidat", e);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Une erreur est survenue lors du refus : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl");
        }
    }

    /**
     * Supprime un candidat
     */
    public void supprimerCandidat(Candidat candidat) {
        try {
            if (candidat == null || candidat.getId() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Candidat invalide."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            Entity entity = entityRepository.findById(candidat.getId()).orElse(null);
            if (entity == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Erreur",
                        "Entité introuvable."));
                PrimeFaces.current().ajax().update(":growl");
                return;
            }
            
            Utilisateur currentUser = loginBean.getCurrentUser();
            String userName = currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "Utilisateur";
            
            entityRepository.delete(entity);
            
            candidatsLoaded = false;
            chargerCandidats();
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le candidat a été supprimé par " + userName + "."));
            PrimeFaces.current().ajax().update(":growl, :candidatsForm");
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du candidat", e);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Une erreur est survenue lors de la suppression : " + e.getMessage()));
            PrimeFaces.current().ajax().update(":growl");
        }
    }

    /**
     * Redirige vers la page de visualisation d'un candidat
     */
    public String visualiserCandidat(Candidat candidat) {
        candidatSelectionne = candidat;
        return "/candidats/view.xhtml?faces-redirect=true";
    }
}
