package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.DescriptionPate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository Spring Data JPA pour l'entité DescriptionPate
 */
@Repository
public interface DescriptionPateRepository extends JpaRepository<DescriptionPate, Long> {

    @Modifying
    @Query("DELETE FROM DescriptionPate dp WHERE dp.entity.id = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}

