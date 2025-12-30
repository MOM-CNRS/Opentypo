package fr.cnrs.opentypo.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.io.Serializable;

/**
 * Entité représentant la relation Many-to-Many entre Entity et Utilisateur (auteur)
 */
@jakarta.persistence.Entity
@Table(name = "auteur")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Auteur implements Serializable {

    @EmbeddedId
    private AuteurId id = new AuteurId();

    // Relation avec Entity
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("entityId")
    @JoinColumn(name = "entity_id", nullable = false)
    private Entity entity;

    // Relation avec Utilisateur
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Utilisateur utilisateur;
}

