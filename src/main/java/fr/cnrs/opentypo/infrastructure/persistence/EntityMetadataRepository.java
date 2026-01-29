package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.EntityMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
