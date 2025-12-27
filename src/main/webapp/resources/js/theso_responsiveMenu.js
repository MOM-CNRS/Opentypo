function toggleSidebar() {
    // Sur mobile, toggle le menu ouvert/fermé
    if (window.innerWidth <= 768) {
        document.body.classList.toggle('menu-open');
    } else {
        // Sur desktop, toggle le collapse/expand
        toggleSidebarCollapse();
    }
}

function toggleSidebarCollapse() {
    const sidebar = document.getElementById('sidebar');
    if (!sidebar) return;
    
    // Toggle la classe collapsed
    sidebar.classList.toggle('sidebar-collapsed');
    
    // Changer l'icône du bouton (chercher dans plusieurs emplacements possibles)
    const toggleButton = document.querySelector('.sidebar-toggle-button .ui-button-icon-left') ||
                        document.querySelector('.sidebar-toggle-button .ui-icon') ||
                        document.querySelector('.sidebar-toggle-button i');
    
    if (toggleButton) {
        const isCollapsed = sidebar.classList.contains('sidebar-collapsed');
        // Retirer toutes les classes d'icônes possibles
        toggleButton.classList.remove('pi-angle-left', 'pi-angle-right', 'pi-angle-double-left', 'pi-angle-double-right');
        
        // Ajouter la bonne icône
        if (isCollapsed) {
            toggleButton.classList.add('pi-angle-right');
        } else {
            toggleButton.classList.add('pi-angle-left');
        }
    }
    
    // Sauvegarder l'état dans localStorage
    localStorage.setItem('sidebarCollapsed', sidebar.classList.contains('sidebar-collapsed'));
}

function closeSidebar() {
    document.body.classList.remove('menu-open');
}

function handleResponsiveMenu() {
    if (window.innerWidth >= 769) {
        // Sur desktop, on peut laisser le menu ouvert ou le fermer selon le besoin
        // document.body.classList.remove('menu-open');
    } else {
        // Sur mobile, on ferme le menu si la fenêtre est redimensionnée
        document.body.classList.remove('menu-open');
    }
}

// Fermer le menu en cliquant sur l'overlay
document.addEventListener('click', function(event) {
    if (window.innerWidth <= 768 && document.body.classList.contains('menu-open')) {
        const sidebar = document.querySelector('.app-sidebar');
        const overlay = event.target;
        
        // Si le clic est en dehors du sidebar
        if (sidebar && !sidebar.contains(event.target) && !event.target.closest('.burger-button')) {
            closeSidebar();
        }
    }
});

// Fermer le menu après avoir cliqué sur un élément du menu (mobile uniquement)
document.addEventListener('DOMContentLoaded', function() {
    const menuLinks = document.querySelectorAll('.sidebar-menu .ui-menuitem-link');
    menuLinks.forEach(link => {
        link.addEventListener('click', function() {
            if (window.innerWidth <= 768) {
                setTimeout(() => {
                    closeSidebar();
                }, 300); // Petit délai pour l'animation
            }
        });
    });
});

window.addEventListener('resize', handleResponsiveMenu);
window.addEventListener('load', handleResponsiveMenu);

// Créer et gérer les tooltips personnalisés pour le menu réduit
let isSettingUpTooltips = false; // Flag pour éviter les boucles infinies

function setupSidebarTooltips() {
    // Éviter les appels multiples simultanés
    if (isSettingUpTooltips) return;
    isSettingUpTooltips = true;
    
    const menuLinks = document.querySelectorAll('.sidebar-menu .ui-menuitem-link');
    menuLinks.forEach(link => {
        // Vérifier si le tooltip existe déjà
        const existingTooltip = link.querySelector('.sidebar-menu-tooltip');
        if (existingTooltip) {
            // Le tooltip existe déjà, ne rien faire
            return;
        }
        
        // Obtenir le texte du menu
        const textElement = link.querySelector('.ui-menuitem-text');
        let tooltipText = '';
        
        if (textElement) {
            tooltipText = textElement.textContent.trim();
        } else {
            // Fallback sur l'attribut title si disponible
            tooltipText = link.getAttribute('title') || link.getAttribute('aria-label') || '';
        }
        
        // Créer le tooltip seulement si on a du texte et qu'il n'existe pas déjà
        if (tooltipText && !existingTooltip) {
            const tooltip = document.createElement('span');
            tooltip.className = 'sidebar-menu-tooltip';
            tooltip.textContent = tooltipText;
            link.style.position = 'relative';
            link.appendChild(tooltip);
        }
    });
    
    isSettingUpTooltips = false;
}

// Restaurer l'état du sidebar au chargement (desktop uniquement)
document.addEventListener('DOMContentLoaded', function() {
    if (window.innerWidth > 768) {
        const sidebar = document.getElementById('sidebar');
        const savedState = localStorage.getItem('sidebarCollapsed');
        
        if (!sidebar) return;
        
        // Chercher l'icône du bouton toggle
        const toggleButton = document.querySelector('.sidebar-toggle-button .ui-button-icon-left') ||
                            document.querySelector('.sidebar-toggle-button .ui-icon') ||
                            document.querySelector('.sidebar-toggle-button i');
        
        if (savedState === 'true') {
            sidebar.classList.add('sidebar-collapsed');
            if (toggleButton) {
                toggleButton.classList.remove('pi-angle-left', 'pi-angle-double-left');
                toggleButton.classList.add('pi-angle-right');
            }
        } else if (savedState === 'false') {
            sidebar.classList.remove('sidebar-collapsed');
            if (toggleButton) {
                toggleButton.classList.remove('pi-angle-right', 'pi-angle-double-right');
                toggleButton.classList.add('pi-angle-left');
            }
        } else {
            // Par défaut, le sidebar est réduit
            sidebar.classList.add('sidebar-collapsed');
            if (toggleButton) {
                toggleButton.classList.remove('pi-angle-left', 'pi-angle-double-left');
                toggleButton.classList.add('pi-angle-right');
            }
        }
    }
    
    // Configurer les tooltips pour le menu
    setupSidebarTooltips();
    
    // Observer les changements du DOM pour mettre à jour les tooltips si le menu change
    // Mais seulement pour les changements qui ne sont pas causés par nos propres tooltips
    const observer = new MutationObserver(function(mutations) {
        // Filtrer les mutations pour éviter les boucles infinies
        const relevantMutations = mutations.filter(mutation => {
            // Ignorer les mutations sur les tooltips que nous avons créés
            if (mutation.target.classList && mutation.target.classList.contains('sidebar-menu-tooltip')) {
                return false;
            }
            // Ignorer les mutations qui ajoutent des tooltips
            if (mutation.addedNodes) {
                for (let node of mutation.addedNodes) {
                    if (node.classList && node.classList.contains('sidebar-menu-tooltip')) {
                        return false;
                    }
                }
            }
            return true;
        });
        
        // Ne mettre à jour que si les mutations sont pertinentes
        if (relevantMutations.length > 0) {
            // Utiliser un délai pour éviter les appels multiples rapides
            clearTimeout(window.tooltipSetupTimeout);
            window.tooltipSetupTimeout = setTimeout(() => {
                setupSidebarTooltips();
            }, 100);
        }
    });
    
    const sidebar = document.getElementById('sidebar');
    if (sidebar) {
        observer.observe(sidebar, {
            childList: true,
            subtree: true,
            attributes: false, // Ne pas observer les changements d'attributs
            characterData: false // Ne pas observer les changements de texte
        });
    }
});



function toggleTreePanel() {
    const tree = document.getElementById('leftTreePanel');
    const iconSpan = document.querySelector('#treeToggleButton .ui-button-icon-left');
    const existingHeader = document.getElementById('treeMobileHeader');

    if (!tree || !iconSpan) return;

    const isVisible = tree.style.display === 'block';

    if (isVisible) {
        // Masquer arbre
        tree.style.display = 'none';

        // Supprimer le header mobile si présent
        if (existingHeader) {
            existingHeader.remove();
        }

        // Restaurer icône
        iconSpan.classList.remove('pi-times');
        iconSpan.classList.add('pi-sitemap');
    } else {
        // Afficher arbre
        tree.style.display = 'block';
        tree.style.position = 'fixed';
        tree.style.top = '50px';
        tree.style.left = '0';
        tree.style.width = '100vw';
        tree.style.height = 'calc(100vh - 50px)';
        tree.style.backgroundColor = '#e8f5e9';
        tree.style.zIndex = '9999';
        tree.style.overflowY = 'auto';

        // Si un ancien header existe (cas improbable), on le retire
        if (existingHeader) {
            existingHeader.remove();
        }

        // Ajouter header avec bouton retour
        const treeHeader = document.createElement('div');
        treeHeader.id = 'treeMobileHeader';
        treeHeader.style.position = 'fixed';
        treeHeader.style.top = '0';
        treeHeader.style.left = '0';
        treeHeader.style.width = '100vw';
        treeHeader.style.height = '50px';
        treeHeader.style.backgroundColor = 'var(--color-action-main)';
        treeHeader.style.display = 'flex';
        treeHeader.style.alignItems = 'center';
        treeHeader.style.justifyContent = 'center';
        treeHeader.style.zIndex = '10000';
        treeHeader.innerHTML = `
            <button onclick="toggleTreePanel()" 
                    style="
                        display: flex;
                        align-items: center;
                        gap: 0.5rem;
                        background-color: white;
                        color: var(--color-action-main);
                        border: none;
                        border-radius: 20px;
                        padding: 6px 12px;
                        font-size: 14px;
                        font-weight: 500;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                        cursor: pointer;
                    ">
                <i class="pi pi-arrow-left" />
                Retour
            </button>
        `;
        document.body.appendChild(treeHeader);

        // Changer icône du bouton principal
        iconSpan.classList.remove('pi-sitemap');
        iconSpan.classList.add('pi-times');
    }
}

