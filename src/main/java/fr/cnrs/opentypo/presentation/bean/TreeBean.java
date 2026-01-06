package fr.cnrs.opentypo.presentation.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.NodeExpandEvent;

import java.io.Serializable;


@Getter
@Setter
@SessionScoped
@Named(value = "treeBean")
public class TreeBean implements Serializable {

    private TreeNode selectedNode;
    private TreeNode root;
    
    // Propriétés pour le formulaire de création de référentiel
    private String referentielCode;
    private String referentielLabel;
    private String referentielDescription;

    @PostConstruct
    public void init() {
        // Racine (invisible)
        root = new DefaultTreeNode("root", null);

        // Niveau 0
        TreeNode ceramique = new DefaultTreeNode("Céramique", root);
        new DefaultTreeNode("Instrumitum", root);
        new DefaultTreeNode("Monnaos", root);

        // Niveau 1 (Céramique)
        TreeNode categorie1 = new DefaultTreeNode("catégorie1", ceramique);
        TreeNode categorie2 = new DefaultTreeNode("catégorie2", ceramique);

        // Niveau 2
        TreeNode groupe1 = new DefaultTreeNode("groupe1", categorie1);
        new DefaultTreeNode("groupe2", categorie1);

        // Niveau 3
        TreeNode serie1 = new DefaultTreeNode("serie1", groupe1);

        // Niveau 4
        new DefaultTreeNode("typo1", serie1);
        new DefaultTreeNode("typo2", serie1);
    }

    public void onNodeSelect(NodeSelectEvent event) {
        this.selectedNode = event.getTreeNode();
    }

    public void onNodeExpand(NodeExpandEvent event) {
        // hook for future lazy loading; no-op for now
    }
    
    public void resetReferentielForm() {
        referentielCode = null;
        referentielLabel = null;
        referentielDescription = null;
    }
    
    public void creerReferentiel() {
        if (referentielCode == null || referentielCode.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Le code du référentiel est requis."));
            PrimeFaces.current().ajax().update(":growl, :referentielForm");
            return;
        }
        
        if (referentielLabel == null || referentielLabel.trim().isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Erreur",
                    "Le label du référentiel est requis."));
            PrimeFaces.current().ajax().update(":growl, :referentielForm");
            return;
        }
        
        // Créer un nouveau nœud référentiel dans l'arbre
        if (root != null) {
            @SuppressWarnings("unchecked")
            TreeNode nouveauReferentiel = new DefaultTreeNode(referentielLabel, root);
            // Le nouveau référentiel est ajouté à l'arbre
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Succès",
                    "Le référentiel '" + referentielLabel + "' a été créé avec succès."));
            
            // Réinitialiser le formulaire
            resetReferentielForm();
            
            PrimeFaces.current().ajax().update(":growl, :referentielForm, :treeWidget");
        }
    }
}
