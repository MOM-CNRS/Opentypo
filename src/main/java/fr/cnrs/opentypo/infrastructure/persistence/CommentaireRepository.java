package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository pour l'entité Commentaire.
 */
@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    List<Commentaire> findByEntity_IdOrderByDateCreationDesc(Long entityId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Commentaire c WHERE c.entity.id = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}
