package fr.cnrs.opentypo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.io.Serializable;

/**
 * Entité représentant une langue
 */
@Entity
@Table(name = "langue")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Langue implements Serializable {

    @Id
    @Column(name = "code", length = 10)
    private String code;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;
}

