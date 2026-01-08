package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.Langue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour l'entité Langue
 */
@Repository
public interface LangueRepository extends JpaRepository<Langue, String> {
    
    /**
     * Récupère toutes les langues triées par nom
     * @return Liste des langues triées par nom
     */
    List<Langue> findAllByOrderByNomAsc();

    Langue findByCode(String code);
}


