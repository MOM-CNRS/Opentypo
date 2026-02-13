package fr.cnrs.opentypo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.io.Serializable;

/**
 * Description spécifique pour les entités de la collection MONNAIE (droit, revers, légendes, coins monétaires).
 */
@jakarta.persistence.Entity
@Audited
@Table(name = "description_monnaie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DescriptionMonnaie implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "droit", columnDefinition = "TEXT")
    private String droit;

    @Column(name = "legende_droit", columnDefinition = "TEXT")
    private String legendeDroit;

    @Column(name = "coins_monetaires_droit", columnDefinition = "TEXT")
    private String coinsMonetairesDroit;

    @Column(name = "revers", columnDefinition = "TEXT")
    private String revers;

    @Column(name = "legende_revers", columnDefinition = "TEXT")
    private String legendeRevers;

    @Column(name = "coins_monetaires_revers", columnDefinition = "TEXT")
    private String coinsMonetairesRevers;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false, unique = true)
    private Entity entity;
}
