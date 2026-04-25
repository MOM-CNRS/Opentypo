package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.InternalAlignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternalAlignmentRepository extends JpaRepository<InternalAlignment, InternalAlignment.InternalAlignmentId> {

    @Query("SELECT ia FROM InternalAlignment ia " +
            "JOIN FETCH ia.targetType t " +
            "LEFT JOIN FETCH t.metadata " +
            "LEFT JOIN FETCH t.labels l " +
            "LEFT JOIN FETCH l.langue " +
            "WHERE ia.sourceType.id = :sourceTypeId")
    List<InternalAlignment> findBySourceTypeIdWithTarget(@Param("sourceTypeId") Long sourceTypeId);

    @Query("SELECT ia.id.targetTypeId FROM InternalAlignment ia WHERE ia.sourceType.id = :sourceTypeId")
    List<Long> findTargetIdsBySourceTypeId(@Param("sourceTypeId") Long sourceTypeId);

    @Modifying
    @Query("DELETE FROM InternalAlignment ia WHERE ia.sourceType.id = :sourceTypeId")
    void deleteBySourceTypeId(@Param("sourceTypeId") Long sourceTypeId);

    @Modifying
    @Query("DELETE FROM InternalAlignment ia WHERE ia.sourceType.id = :sourceTypeId AND ia.targetType.id = :targetTypeId")
    void deleteBySourceTypeIdAndTargetTypeId(@Param("sourceTypeId") Long sourceTypeId, @Param("targetTypeId") Long targetTypeId);
}
