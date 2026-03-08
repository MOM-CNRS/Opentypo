package fr.cnrs.opentypo.presentation.bean;

import jakarta.servlet.http.Part;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Holder pour fichiers multiples. Classe simple (non proxied) pour éviter
 * PropertyNotWritableException avec le proxy CGLIB sur EntityUpdateBean.
 */
public class PendingFilePartsHolder implements Serializable {

    private transient List<Part> parts = new ArrayList<>();

    public List<Part> getParts() {
        return parts != null ? parts : new ArrayList<>();
    }

    public void setParts(Collection<Part> parts) {
        this.parts = parts != null ? new ArrayList<>(parts) : new ArrayList<>();
    }
}
