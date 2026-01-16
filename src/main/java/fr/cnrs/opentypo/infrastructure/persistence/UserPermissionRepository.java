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
     * Supprime toutes les permissions d'un utilisateur
     * 
     * @param utilisateur L'utilisateur
     */
    @Modifying
    @Query("DELETE FROM UserPermission up WHERE up.utilisateur = :utilisateur")
    void deleteByUtilisateur(@Param("utilisateur") Utilisateur utilisateur);
}
