package fr.cnrs.opentypo.presentation.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Élément d'image en cours d'édition (URL + légende).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditingImageItem implements Serializable {

    private String url;
    private String legende;
}
