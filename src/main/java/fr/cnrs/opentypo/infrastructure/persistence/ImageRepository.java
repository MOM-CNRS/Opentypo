package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Image;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository pour l'entité Image.
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByEntity_Id(@Param("entityId") Long entityId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Image i WHERE i.entity.id = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}
