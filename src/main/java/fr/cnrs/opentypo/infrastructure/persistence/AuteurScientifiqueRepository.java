package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.AuteurScientifique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuteurScientifiqueRepository extends JpaRepository<AuteurScientifique, Long> {

    List<AuteurScientifique> findAllByOrderByNomAscPrenomAsc();

    Optional<AuteurScientifique> findFirstByNomIgnoreCaseAndPrenomIgnoreCaseOrderByIdAsc(String nom, String prenom);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM entity_auteur_scientifique
            WHERE auteur_scientifique_id = :authorId
            """, nativeQuery = true)
    void deleteEntityLinksByAuthorId(@Param("authorId") Long authorId);
}
