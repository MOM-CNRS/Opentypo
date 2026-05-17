package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.DescriptionPate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository Spring Data JPA pour l'entité DescriptionPate
 */
@Repository
public interface DescriptionPateRepository extends JpaRepository<DescriptionPate, Long> {

    Optional<DescriptionPate> findByEntity_Id(Long entityId);

    @Modifying
    @Transactional
    @Query("DELETE FROM DescriptionPate dp WHERE dp.entity.id = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}

