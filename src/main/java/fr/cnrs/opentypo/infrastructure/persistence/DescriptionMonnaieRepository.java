package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.DescriptionMonnaie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repository Spring Data JPA pour l'entité Groupe
 */
@Repository
public interface DescriptionMonnaieRepository extends JpaRepository<DescriptionMonnaie, Long> {

}

