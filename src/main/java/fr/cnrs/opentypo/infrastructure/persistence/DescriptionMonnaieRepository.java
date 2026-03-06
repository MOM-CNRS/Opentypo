package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.DescriptionMonnaie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository Spring Data JPA pour l'entité DescriptionMonnaie
 */
@Repository
public interface DescriptionMonnaieRepository extends JpaRepository<DescriptionMonnaie, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM DescriptionMonnaie dm WHERE dm.entity.id = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}

