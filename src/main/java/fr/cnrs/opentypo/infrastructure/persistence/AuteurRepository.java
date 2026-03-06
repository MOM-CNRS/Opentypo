package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Auteur;
import fr.cnrs.opentypo.domain.entity.AuteurId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository pour l'entité Auteur (table de jointure Entity <-> Utilisateur).
 */
@Repository
public interface AuteurRepository extends JpaRepository<Auteur, AuteurId> {

    @Modifying
    @Transactional
    @Query("DELETE FROM Auteur a WHERE a.id.entityId = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}
