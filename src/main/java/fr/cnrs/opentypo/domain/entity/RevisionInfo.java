package fr.cnrs.opentypo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Entité représentant les informations de révision Hibernate Envers
 * Cette table est créée automatiquement par Hibernate Envers
 */
@Entity
@Table(name = "revinfo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevisionInfo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rev")
    private Long rev;

    @Column(name = "revtstmp")
    private Long revtstmp;

    /**
     * Retourne la date de révision en LocalDateTime
     */
    public LocalDateTime getRevisionDate() {
        if (revtstmp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(revtstmp),
            ZoneId.systemDefault()
        );
    }
}
