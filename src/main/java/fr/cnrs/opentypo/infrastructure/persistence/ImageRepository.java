package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository pour l'entité Image.
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    @Modifying
    @Query("DELETE FROM Image i WHERE i.entity.id = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}
