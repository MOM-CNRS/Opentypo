package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.SitePresentation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SitePresentationRepository extends JpaRepository<SitePresentation, Long> {

    Optional<SitePresentation> findByLangue_Code(String langueCode);

    List<SitePresentation> findAllByOrderByLangue_CodeAsc();
}
