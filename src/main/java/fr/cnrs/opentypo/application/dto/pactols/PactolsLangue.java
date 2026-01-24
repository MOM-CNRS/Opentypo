package fr.cnrs.opentypo.application.dto.pactols;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO pour représenter une langue disponible dans un thésaurus PACTOLS
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PactolsLangue implements Serializable {
    
    private String idLang;
    private String nom; // Nom de la langue
}
