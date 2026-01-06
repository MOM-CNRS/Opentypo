package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Groupe;
import fr.cnrs.opentypo.domain.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité Utilisateur
 */
@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    /**
     * Trouve un utilisateur par son email
     * 
     * @param email L'email de l'utilisateur
     * @return L'utilisateur trouvé ou Optional.empty() si aucun utilisateur n'est trouvé
     */
    Optional<Utilisateur> findByEmail(String email);

    /**
     * Vérifie si un utilisateur existe avec l'email donné
     * 
     * @param email L'email à vérifier
     * @return true si un utilisateur existe avec cet email, false sinon
     */
    boolean existsByEmail(String email);

    /**
     * Trouve tous les utilisateurs appartenant à un groupe donné
     * 
     * @param groupe Le groupe
     * @return Liste des utilisateurs du groupe
     */
    List<Utilisateur> findByGroupe(Groupe groupe);

    /**
     * Trouve tous les utilisateurs appartenant à un groupe par son ID
     * 
     * @param groupeId L'ID du groupe
     * @return Liste des utilisateurs du groupe
     */
    List<Utilisateur> findByGroupeId(Long groupeId);

    /**
     * Trouve tous les utilisateurs appartenant à un groupe par son nom
     * 
     * @param groupeNom Le nom du groupe
     * @return Liste des utilisateurs du groupe
     */
    @Query("SELECT u FROM Utilisateur u WHERE u.groupe.nom = :groupeNom")
    List<Utilisateur> findByGroupeNom(@Param("groupeNom") String groupeNom);

    /**
     * Recherche des utilisateurs par nom (insensible à la casse)
     * 
     * @param nom Le nom à rechercher
     * @return Liste des utilisateurs correspondants
     */
    List<Utilisateur> findByNomContainingIgnoreCase(String nom);

    /**
     * Recherche des utilisateurs par prénom (insensible à la casse)
     * 
     * @param prenom Le prénom à rechercher
     * @return Liste des utilisateurs correspondants
     */
    List<Utilisateur> findByPrenomContainingIgnoreCase(String prenom);

    /**
     * Recherche des utilisateurs par nom ou prénom (insensible à la casse)
     * 
     * @param nom Le nom à rechercher
     * @param prenom Le prénom à rechercher
     * @return Liste des utilisateurs correspondants
     */
    @Query("SELECT u FROM Utilisateur u WHERE LOWER(u.nom) LIKE LOWER(CONCAT('%', :nom, '%')) OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))")
    List<Utilisateur> findByNomOrPrenomContainingIgnoreCase(@Param("nom") String nom, @Param("prenom") String prenom);

    /**
     * Recherche des utilisateurs par email (insensible à la casse)
     * 
     * @param email L'email à rechercher
     * @return Liste des utilisateurs correspondants
     */
    @Query("SELECT u FROM Utilisateur u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))")
    List<Utilisateur> findByEmailContainingIgnoreCase(@Param("email") String email);

    /**
     * Compte le nombre d'utilisateurs dans un groupe
     * 
     * @param groupeId L'ID du groupe
     * @return Le nombre d'utilisateurs dans le groupe
     */
    long countByGroupeId(Long groupeId);

    /**
     * Trouve un utilisateur par email et mot de passe hash
     * (Utile pour l'authentification)
     * 
     * @param email L'email de l'utilisateur
     * @param passwordHash Le hash du mot de passe
     * @return L'utilisateur trouvé ou Optional.empty() si aucun utilisateur n'est trouvé
     */
    Optional<Utilisateur> findByEmailAndPasswordHash(String email, String passwordHash);
}

