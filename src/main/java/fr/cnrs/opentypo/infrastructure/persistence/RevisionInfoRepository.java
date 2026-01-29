package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.RevisionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour la table REVINFO de Hibernate Envers
 */
@Repository
public interface RevisionInfoRepository extends JpaRepository<RevisionInfo, Long> {
}
