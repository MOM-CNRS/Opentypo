package fr.cnrs.opentypo.entity;

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
import org.hibernate.envers.Audited;

import java.io.Serializable;

/**
 * Entité représentant un type d'entité (Référentiel, Catégorie, Groupe, Série, Type)
 */
@Entity
@Table(name = "entity_type")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntityType implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;
}

