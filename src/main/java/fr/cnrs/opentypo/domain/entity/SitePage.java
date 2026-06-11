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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.io.Serializable;

/**
 * Contenu d'une page statique du site (contact, mentions légales, accessibilité), par langue.
 */
@Entity
@Table(
        name = "site_page",
        uniqueConstraints = @UniqueConstraint(name = "uq_site_page_code_langue", columnNames = {"page_code", "langue_code"})
)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SitePage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_code", nullable = false, length = 50)
    private String pageCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "langue_code", nullable = false)
    private Langue langue;

    @Column(name = "titre", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String titre;

    @Column(name = "contenu", columnDefinition = "TEXT")
    private String contenu;
}
