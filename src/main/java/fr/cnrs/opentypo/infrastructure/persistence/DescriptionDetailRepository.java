package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.DescriptionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository Spring Data JPA pour l'entité DescriptionDetail
 */
@Repository
public interface DescriptionDetailRepository extends JpaRepository<DescriptionDetail, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM DescriptionDetail dd WHERE dd.entity.id = :entityId")
    void deleteByEntityId(@Param("entityId") Long entityId);
}

