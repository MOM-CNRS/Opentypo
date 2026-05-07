package fr.cnrs.opentypo.application.dto.zotero;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZoteroCollectionOption implements Serializable {
    private static final long serialVersionUID = 1L;
    private String key;
    private String name;
}
