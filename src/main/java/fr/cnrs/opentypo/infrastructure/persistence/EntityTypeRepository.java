package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité EntityType
 */
@Repository
public interface EntityTypeRepository extends JpaRepository<EntityType, Long> {

    /**
     * Trouve un type d'entité par son code
     */
    Optional<EntityType> findByCode(String code);

    /**
     * Vérifie si un type d'entité existe avec le code donné
     */
    boolean existsByCode(String code);
}

