package fr.cnrs.opentypo.domain.entity;

import fr.cnrs.opentypo.common.constant.EntityConstants;
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
@Table(name = "auteur_scientifique")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuteurScientifique implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String nom;

    @Column(name = "prenom", nullable = false, length = EntityConstants.VARCHAR_COLUMN_MAX_LENGTH)
    private String prenom;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
