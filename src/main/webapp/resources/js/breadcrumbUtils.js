/**
 * Fonction pour basculer l'état du breadcrumb (déplié/replié)
 */
function toggleBreadcrumb() {
    const content = document.getElementById('breadcrumbContent');
    const icon = document.getElementById('breadcrumbToggleIcon');
    
    if (content && icon) {
        const isExpanded = content.classList.contains('breadcrumb-expanded');
        
        if (isExpanded) {
            content.classList.remove('breadcrumb-expanded');
            icon.querySelector('i').classList.remove('pi-chevron-up');
            icon.querySelector('i').classList.add('pi-chevron-down');
        } else {
            content.classList.add('breadcrumb-expanded');
            icon.querySelector('i').classList.remove('pi-chevron-down');
            icon.querySelector('i').classList.add('pi-chevron-up');
        }
    }
}

// Initialiser l'état (fermé par défaut)
document.addEventListener('DOMContentLoaded', function() {
    const content = document.getElementById('breadcrumbContent');
    if (content) {
        content.classList.remove('breadcrumb-expanded');
    }
});
