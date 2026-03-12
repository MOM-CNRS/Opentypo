/**
 * Gestion des animations de glissement du panneau d'actions modifier.
 * - Slide up : à l'apparition du panneau (géré par CSS)
 * - Slide down : au clic sur Retour ou Enregistrer, puis exécution de l'action
 */
function modifierActionsSlideOut(action) {
    var wrapper = document.getElementById('modifierActionsWrapper') ||
                  document.querySelector('.modifier-actions-wrapper.modifier-actions-sticky');
    var bar = document.getElementById('modifierActionsBar') ||
              (wrapper && wrapper.querySelector('.collection-modifier-actions'));

    if (!wrapper || !bar) {
        // Fallback si les éléments ne sont pas trouvés (préfixe JSF)
        wrapper = document.querySelector('.modifier-actions-wrapper.modifier-actions-sticky');
        bar = wrapper && wrapper.querySelector('.collection-modifier-actions');
    }

    if (wrapper && bar) {
        /* Légère latence pour laisser le feedback visuel du clic s'afficher */
        requestAnimationFrame(function() {
            wrapper.classList.add('modifier-actions-sliding-down');
        });

        var handler = function() {
            bar.removeEventListener('animationend', handler);
            if (action === 'retour' && typeof modifierCancelCommand === 'function') {
                modifierCancelCommand();
            } else if (action === 'enregistrer' && typeof modifierPrepareSaveCommand === 'function') {
                modifierPrepareSaveCommand();
            }
        };

        bar.addEventListener('animationend', handler);

        // Sécurité : si l'animation ne se déclenche pas (déjà caché, etc.)
        setTimeout(function() {
            if (wrapper.classList.contains('modifier-actions-sliding-down')) {
                bar.removeEventListener('animationend', handler);
                if (action === 'retour' && typeof modifierCancelCommand === 'function') {
                    modifierCancelCommand();
                } else if (action === 'enregistrer' && typeof modifierPrepareSaveCommand === 'function') {
                    modifierPrepareSaveCommand();
                }
            }
        }, 600);
    } else {
        // Fallback direct si structure non trouvée
        if (action === 'retour' && typeof modifierCancelCommand === 'function') {
            modifierCancelCommand();
        } else if (action === 'enregistrer' && typeof modifierPrepareSaveCommand === 'function') {
            modifierPrepareSaveCommand();
        }
    }
}
