package fr.cnrs.opentypo.presentation.bean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Named("entityEditModeBean")
@SessionScoped
@Getter
@Setter
public class EntityEditModeBean implements Serializable {

    private boolean editingEntityInCatalog = false;

    public void startEditing() { editingEntityInCatalog = true; }
    public void cancelEditing() { editingEntityInCatalog = false; }
}
