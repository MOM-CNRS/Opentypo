package fr.cnrs.opentypo.presentation.bean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;


@Named("searchBean")
@SessionScoped
@Getter
@Setter
public class SearchBean implements Serializable {

    private String searchSelected;
    private String typeSelected;
    private String langSelected;


    public void applySearch() {

    }

}

