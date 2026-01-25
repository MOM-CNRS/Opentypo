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
import org.hibernate.envers.Audited;

import java.io.Serializable;


@Entity
@Audited
@Table(name = "reference-opentheso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceOpentheso implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "valeur", nullable = false, length = 500)
    private String valeur;

    @Column(name = "thesaurus_id")
    private String thesaurusId;

    @Column(name = "concept_id")
    private String conceptId;

    @Column(name = "collection_id")
    private String collectionId;

    @Column(name = "url", length = 500)
    private String url;
}