package fr.cnrs.opentypo.domain.entity;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.io.Serializable;


@Entity
@Audited
@Table(name = "reference-opentheso")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceOpentheso implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String code;

    @Column(name = "valeur", nullable = false, length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String valeur;

    @Column(name = "thesaurus_id", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String thesaurusId;

    @Column(name = "concept_id", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String conceptId;

    @Column(name = "collection_id", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String collectionId;

    @Column(name = "url", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    private fr.cnrs.opentypo.domain.entity.Entity entity;
}