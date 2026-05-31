package fr.cnrs.opentypo.domain.entity;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.io.Serializable;

/**
 * Entité représentant la description de pâte multilingue
 */
@Entity
@Table(name = "description_pate")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DescriptionPate implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String description;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false, unique = true)
    private fr.cnrs.opentypo.domain.entity.Entity entity;

}

