package fr.cnrs.opentypo.application.dto.pactols;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO pour représenter un concept PACTOLS
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PactolsConcept implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String idConcept;
    private List<LinkedHashMap<String, String>> terms; // Map<langue, terme>
    private String selectedTerm; // Terme sélectionné selon la langue choisie
}
