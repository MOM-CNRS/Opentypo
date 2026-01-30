package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Entity;
import fr.cnrs.opentypo.domain.entity.EntityRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Spring Data JPA pour l'entité EntityRelation
 */
@Repository
public interface EntityRelationRepository extends JpaRepository<EntityRelation, EntityRelation.EntityRelationId> {

    /**
     * Trouve toutes les relations où l'entité donnée est parent
     */
    List<EntityRelation> findByParent(Entity parent);

    /**
     * Trouve toutes les relations où l'entité donnée est enfant
     */
    List<EntityRelation> findByChild(Entity child);

    /**
     * Trouve tous les enfants d'une entité parent
     */
    @Query("SELECT er.child FROM EntityRelation er WHERE er.parent = :parent")
    List<Entity> findChildrenByParent(@Param("parent") Entity parent);

    /**
     * Trouve tous les parents d'une entité enfant
     */
    @Query("SELECT er.parent FROM EntityRelation er WHERE er.child = :child")
    List<Entity> findParentsByChild(@Param("child") Entity child);

    /**
     * Trouve tous les enfants d'un type spécifique pour un parent donné
     */
    @Query("SELECT er.child FROM EntityRelation er WHERE er.parent = :parent AND er.child.entityType.code = :typeCode")
    List<Entity> findChildrenByParentAndType(@Param("parent") Entity parent, @Param("typeCode") String typeCode);

    /**
     * Vérifie si une relation existe entre un parent et un enfant
     */
    @Query("SELECT COUNT(er) > 0 FROM EntityRelation er WHERE er.parent.id = :parentId AND er.child.id = :childId")
    boolean existsByParentAndChild(@Param("parentId") Long parentId, @Param("childId") Long childId);

    /**
     * Retourne toutes les relations (parent_id, child_id) du sous-arbre dont la racine est l'entité donnée.
     * Utilise une CTE récursive (PostgreSQL). Chaque ligne est (parent_id, child_id).
     */
    @Query(value = """
        WITH RECURSIVE subtree AS (
            SELECT parent_id, child_id FROM entity_relation WHERE parent_id = :rootId
            UNION ALL
            SELECT r.parent_id, r.child_id FROM entity_relation r
            INNER JOIN subtree s ON r.parent_id = s.child_id
        )
        SELECT parent_id, child_id FROM subtree
        """, nativeQuery = true)
    List<Object[]> findAllDescendantRelations(@Param("rootId") Long rootId);
}
