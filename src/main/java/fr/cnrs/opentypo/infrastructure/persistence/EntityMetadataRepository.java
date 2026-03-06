package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.EntityMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité EntityMetadata
 */
@Repository
public interface EntityMetadataRepository extends JpaRepository<EntityMetadata, Long> {

    /**
     * Trouve les métadonnées par le code de l'entité
     */
    Optional<EntityMetadata> findByCode(String code);

    /**
     * Trouve les métadonnées par l'ID de l'entité
     */
    Optional<EntityMetadata> findByEntityId(Long entityId);

    @Modifying
    @Transactional
    @Query("DELETE FROM EntityMetadata em WHERE em.entity.id = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}
