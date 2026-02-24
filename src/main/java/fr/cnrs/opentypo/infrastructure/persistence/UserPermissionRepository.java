package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.UserPermission;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Spring Data JPA pour l'entité UserPermission
 */
@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, UserPermission.UserPermissionId> {

    /**
     * Trouve toutes les permissions d'un utilisateur
     * 
     * @param utilisateur L'utilisateur
     * @return Liste des permissions de l'utilisateur
     */
    List<UserPermission> findByUtilisateur(Utilisateur utilisateur);

    /**
     * Trouve les IDs des utilisateurs ayant un rôle donné sur une entité
     */
    @Query("SELECT up.id.userId FROM UserPermission up WHERE up.id.entityId = :entityId AND up.role = :role")
    List<Long> findUserIdsByEntityIdAndRole(@Param("entityId") Long entityId, @Param("role") String role);

    /**
     * Vérifie si un utilisateur a un rôle donné sur une entité
     */
    @Query("SELECT CASE WHEN COUNT(up) > 0 THEN true ELSE false END FROM UserPermission up WHERE up.id.userId = :userId AND up.id.entityId = :entityId AND up.role = :role")
    boolean existsByUserIdAndEntityIdAndRole(@Param("userId") Long userId,
                                             @Param("entityId") Long entityId,
                                             @Param("role") String role);

    /**
     * Vérifie si un utilisateur a une permission (n'importe quel rôle) sur une entité
     */
    @Query("SELECT CASE WHEN COUNT(up) > 0 THEN true ELSE false END FROM UserPermission up WHERE up.id.userId = :userId AND up.id.entityId = :entityId")
    boolean existsByUserIdAndEntityId(@Param("userId") Long userId, @Param("entityId") Long entityId);

    /**
     * Supprime toutes les permissions d'un rôle donné pour une entité
     */
    @Modifying
    @Query("DELETE FROM UserPermission up WHERE up.id.entityId = :entityId AND up.role = :role")
    void deleteByEntityIdAndRole(@Param("entityId") Long entityId, @Param("role") String role);

    /**
     * Supprime toutes les permissions d'un utilisateur
     * 
     * @param utilisateur L'utilisateur
     */
    @Modifying
    @Query("DELETE FROM UserPermission up WHERE up.utilisateur = :utilisateur")
    void deleteByUtilisateur(@Param("utilisateur") Utilisateur utilisateur);
}
