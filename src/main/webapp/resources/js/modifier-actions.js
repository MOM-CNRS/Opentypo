/**
 * Panneau d'actions du formulaire « modifier ».
 * La barre reste fixe en bas d'écran (CSS) et ne disparaît pas au clic : les actions
 * sont exécutées directement ; la barre n'est retirée du DOM qu'à la sortie du mode
 * édition (mise à jour JSF après annulation / enregistrement).
 */
function modifierActionsSlideOut(action) {
    if (action === 'retour' && typeof modifierCancelCommand === 'function') {
        modifierCancelCommand();
    } else if (action === 'enregistrer' && typeof modifierPrepareSaveCommand === 'function') {
        modifierPrepareSaveCommand();
    }
}
