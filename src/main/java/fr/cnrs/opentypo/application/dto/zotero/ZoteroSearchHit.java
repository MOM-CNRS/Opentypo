package fr.cnrs.opentypo.application.dto.zotero;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Suggestion d'item Zotero pour autocomplétion (propriétés JavaBean pour EL JSF).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZoteroSearchHit implements Serializable {
    private static final long serialVersionUID = 1L;
    private String key;
    private String label;
}
