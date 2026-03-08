package fr.cnrs.opentypo.presentation.bean;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

import jakarta.servlet.http.Part;

/**
 * Emplacement pour un fichier en attente d'upload.
 * Le fichier reste dans le navigateur jusqu'au clic sur "Enregistrer".
 */
@Getter
@Setter
public class PendingFileSlot implements Serializable {

    private Part part;

    public PendingFileSlot() {
    }
}
