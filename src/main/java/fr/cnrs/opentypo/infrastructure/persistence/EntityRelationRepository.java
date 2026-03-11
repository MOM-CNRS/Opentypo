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
     * Trouve tous les enfants d'une entité parent, ordonnés par display_order puis par code.
     * Ordre par défaut alphabétique quand display_order est null.
     */
    @Query("""
        SELECT er.child FROM EntityRelation er
        LEFT JOIN FETCH er.child.entityType
        WHERE er.parent = :parent
        ORDER BY COALESCE(er.displayOrder, 999999) ASC, LOWER(COALESCE(er.child.metadata.code, '')) ASC
        """)
    List<Entity> findChildrenByParentOrdered(@Param("parent") Entity parent);

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
     * Trouve toutes les relations parent-enfant pour un type d'entité donné, ordonnées par display_order
     * (NULL en dernier = ordre alphabétique par code), puis par code de l'enfant.
     */
    @Query("""
        SELECT er FROM EntityRelation er
        WHERE er.parent = :parent AND er.child.entityType.code = :typeCode
        ORDER BY COALESCE(er.displayOrder, 999999) ASC, LOWER(er.child.metadata.code) ASC
        """)
    List<EntityRelation> findRelationsByParentAndTypeOrdered(@Param("parent") Entity parent, @Param("typeCode") String typeCode);

    /**
     * Trouve une relation par parent et enfant
     */
    @Query("SELECT er FROM EntityRelation er WHERE er.parent.id = :parentId AND er.child.id = :childId")
    java.util.Optional<EntityRelation> findByParentIdAndChildId(@Param("parentId") Long parentId, @Param("childId") Long childId);

    /**
     * Vérifie si une relation existe entre un parent et un enfant
     */
    @Query("SELECT COUNT(er) > 0 FROM EntityRelation er WHERE er.parent.id = :parentId AND er.child.id = :childId")
    boolean existsByParentAndChild(@Param("parentId") Long parentId, @Param("childId") Long childId);

    /**
     * Retourne toutes les relations (parent_id, child_id, display_order) du sous-arbre dont la racine est l'entité donnée.
     * Utilise une CTE récursive (PostgreSQL). Chaque ligne est (parent_id, child_id, display_order).
     * display_order NULL est retourné comme 999999 pour le tri.
     */
    @Query(value = """
        WITH RECURSIVE subtree AS (
            SELECT parent_id, child_id, display_order FROM entity_relation WHERE parent_id = :rootId
            UNION ALL
            SELECT r.parent_id, r.child_id, r.display_order FROM entity_relation r
            INNER JOIN subtree s ON r.parent_id = s.child_id
        )
        SELECT parent_id, child_id, COALESCE(display_order, 999999) FROM subtree
        """, nativeQuery = true)
    List<Object[]> findAllDescendantRelations(@Param("rootId") Long rootId);
}
