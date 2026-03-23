package fr.cnrs.opentypo.infrastructure.listener;

import fr.cnrs.opentypo.domain.entity.RevisionInfo;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Listener Envers qui enregistre l'utilisateur courant dans chaque révision.
 */
public class OpentypoRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        RevisionInfo rev = (RevisionInfo) revisionEntity;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            rev.setModifiedBy(auth.getName());
        } else {
            rev.setModifiedBy(null);
        }
    }
}
