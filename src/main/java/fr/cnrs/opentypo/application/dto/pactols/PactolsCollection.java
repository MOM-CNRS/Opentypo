package fr.cnrs.opentypo.application.dto.pactols;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO pour représenter une collection PACTOLS
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PactolsCollection implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String idGroup;
    private List<LinkedHashMap<String, String>> labels; // Map<langue, label>
    private String selectedLabel; // Label sélectionné selon la langue choisie
}
