package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.CaracteristiquePhysique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repository Spring Data JPA pour l'entité Groupe
 */
@Repository
public interface CaracteristiquePhysiqueRepository extends JpaRepository<CaracteristiquePhysique, Long> {

}

