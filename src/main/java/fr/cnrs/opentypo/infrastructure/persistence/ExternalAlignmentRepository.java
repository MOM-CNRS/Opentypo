package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.ExternalAlignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExternalAlignmentRepository extends JpaRepository<ExternalAlignment, Long> {

    List<ExternalAlignment> findBySourceType_IdOrderByIdAsc(Long sourceTypeId);

    @Modifying
    @Query("DELETE FROM ExternalAlignment ea WHERE ea.sourceType.id = :sourceTypeId")
    void deleteBySourceTypeId(@Param("sourceTypeId") Long sourceTypeId);
}
