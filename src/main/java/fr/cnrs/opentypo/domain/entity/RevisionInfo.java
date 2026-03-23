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
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;

import fr.cnrs.opentypo.infrastructure.listener.OpentypoRevisionListener;

/**
 * Entité représentant les informations de révision Hibernate Envers.
 * Stocke le numéro de révision, la date et l'utilisateur ayant effectué la modification.
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(OpentypoRevisionListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevisionInfo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    private Long rev;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private Long revtstmp;

    /**
     * Email ou identifiant de l'utilisateur ayant effectué la modification.
     * Null si opération non authentifiée (ex: import batch).
     */
    @Column(name = "modified_by", length = 255)
    private String modifiedBy;

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
