package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.AuteurScientifique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuteurScientifiqueRepository extends JpaRepository<AuteurScientifique, Long> {

    List<AuteurScientifique> findAllByOrderByNomAscPrenomAsc();
}
