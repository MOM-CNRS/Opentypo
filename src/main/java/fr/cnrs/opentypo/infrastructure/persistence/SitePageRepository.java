package fr.cnrs.opentypo.infrastructure.persistence;

import fr.cnrs.opentypo.domain.entity.SitePage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SitePageRepository extends JpaRepository<SitePage, Long> {

    Optional<SitePage> findByPageCodeAndLangue_Code(String pageCode, String langueCode);
}
