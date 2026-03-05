package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour l'entité Label.
 */
@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {

    List<Label> findByEntity_Id(Long entityId);

    @Modifying
    @Query("DELETE FROM Label l WHERE l.entity.id = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}
