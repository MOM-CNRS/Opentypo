package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Description;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository pour l'entité Description.
 */
@Repository
public interface DescriptionRepository extends JpaRepository<Description, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM Description d WHERE d.entity.id = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}
