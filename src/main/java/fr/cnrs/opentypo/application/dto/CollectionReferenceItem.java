package fr.cnrs.opentypo.application.dto;

import fr.cnrs.opentypo.domain.entity.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe interne pour représenter une collection avec ses référentiels
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CollectionReferenceItem implements Serializable {

    private Entity collection;
    private List<Entity> references = new ArrayList<>();
    private boolean collectionSelected = false;
    private List<Boolean> referencesSelected = new ArrayList<>();
}