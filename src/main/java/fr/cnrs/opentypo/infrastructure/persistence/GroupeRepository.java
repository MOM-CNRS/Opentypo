package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Groupe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité Groupe
 */
@Repository
public interface GroupeRepository extends JpaRepository<Groupe, Long> {

    /**
     * Trouve un groupe par son nom
     * 
     * @param nom Le nom du groupe
     * @return Le groupe trouvé ou Optional.empty()
     */
    Optional<Groupe> findByNom(String nom);

    /**
     * Vérifie si un groupe existe avec le nom donné
     * 
     * @param nom Le nom à vérifier
     * @return true si un groupe existe avec ce nom, false sinon
     */
    boolean existsByNom(String nom);
}

