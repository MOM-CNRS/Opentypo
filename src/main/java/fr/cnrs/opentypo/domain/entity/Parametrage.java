package fr.cnrs.opentypo.domain.entity;

import fr.cnrs.opentypo.common.constant.EntityConstants;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Paramétrage OpenTheso lié à une entité (collection).
 * Stocke l'URL de base, l'id thésaurus, l'id groupe (collection) et l'id langue.
 */
@jakarta.persistence.Entity
@Table(name = "parametrage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Parametrage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false, unique = true)
    private fr.cnrs.opentypo.domain.entity.Entity entity;

    @Column(name = "base_url", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String baseUrl;

    @Column(name = "id_theso", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String idTheso;

    @Column(name = "id_groupe", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String idGroupe;

    @Column(name = "id_langue", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String idLangue;

    @Column(name = "bibliographie_url", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String bibliographieUrl;

    /** NAAN ARK pour ce référentiel (prioritaire sur la config application si renseigné). */
    @Column(name = "ark_naan", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String arkNaan;

    /** Épaule / préfixe local ARK (prioritaire sur la config application si renseigné). */
    @Column(name = "ark_shoulder", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String arkShoulder;

    /**
     * Base du résolveur ARK (ex. https://n2t.net), pour liens « ouvrir » depuis l'interface ; optionnel.
     */
    @Column(name = "ark_resolver_base", length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String arkResolverBase;
}
