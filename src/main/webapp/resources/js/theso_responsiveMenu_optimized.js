/**
 * Script optimisé pour la gestion du menu responsive
 * Utilise les constantes et utilitaires définis dans constants.js et sidebarUtils.js
 */

(function() {
    'use strict';

    // Variables privées
    let isSettingUpTooltips = false;
    let tooltipSetupTimeout = null;

    /**
     * Toggle la sidebar (mobile ou desktop)
     */
    function toggleSidebar() {
        if (window.innerWidth <= AppConstants.MOBILE_BREAKPOINT) {
            document.body.classList.toggle(AppConstants.CSS.MENU_OPEN);
        } else {
            toggleSidebarCollapse();
        }
    }

    /**
     * Toggle le collapse/expand de la sidebar (desktop)
     */
    function toggleSidebarCollapse() {
        const sidebar = SidebarUtils.getSidebar();
        if (!sidebar) return;
        
        sidebar.classList.toggle(AppConstants.CSS.SIDEBAR_COLLAPSED);
        const isCollapsed = sidebar.classList.contains(AppConstants.CSS.SIDEBAR_COLLAPSED);
        
        SidebarUtils.updateToggleIcon(isCollapsed);
        SidebarUtils.saveSidebarState(isCollapsed);
    }

    /**
     * Ferme la sidebar (mobile)
     */
    function closeSidebar() {
        document.body.classList.remove(AppConstants.CSS.MENU_OPEN);
    }

    /**
     * Gère le menu responsive lors du redimensionnement
     */
    function handleResponsiveMenu() {
        if (window.innerWidth <= AppConstants.MOBILE_BREAKPOINT) {
            document.body.classList.remove(AppConstants.CSS.MENU_OPEN);
        }
    }

    /**
     * Configure les tooltips pour le menu réduit
     */
    function setupSidebarTooltips() {
        if (isSettingUpTooltips) return;
        isSettingUpTooltips = true;
        
        const menuLinks = document.querySelectorAll(AppConstants.CSS.MENU_LINK);
        menuLinks.forEach(link => {
            if (link.querySelector(`.${AppConstants.CSS.TOOLTIP}`)) {
                return; // Tooltip déjà présent
            }
            
            const textElement = link.querySelector('.ui-menuitem-text');
            const tooltipText = textElement 
                ? textElement.textContent.trim() 
                : (link.getAttribute('title') || link.getAttribute('aria-label') || '');
            
            if (tooltipText) {
                const tooltip = document.createElement('span');
                tooltip.className = AppConstants.CSS.TOOLTIP;
                tooltip.textContent = tooltipText;
                link.style.position = 'relative';
                link.appendChild(tooltip);
            }
        });
        
        isSettingUpTooltips = false;
    }

    /**
     * Gère le clic sur l'overlay pour fermer le menu mobile
     */
    function handleOverlayClick(event) {
        if (window.innerWidth > AppConstants.MOBILE_BREAKPOINT) return;
        if (!document.body.classList.contains(AppConstants.CSS.MENU_OPEN)) return;
        
        const sidebar = document.querySelector(`.${AppConstants.CSS.SIDEBAR}`);
        if (sidebar && !sidebar.contains(event.target) 
            && !event.target.closest(`.${AppConstants.CSS.BURGER_BUTTON}`)) {
            closeSidebar();
        }
    }

    /**
     * Ferme le menu après clic sur un lien (mobile uniquement)
     */
    function handleMenuLinkClick() {
        if (window.innerWidth <= AppConstants.MOBILE_BREAKPOINT) {
            setTimeout(closeSidebar, AppConstants.MENU_CLOSE_DELAY);
        }
    }

    /**
     * Initialise les tooltips avec un observer pour les changements DOM
     */
    function initializeTooltipsObserver() {
        const observer = new MutationObserver(function(mutations) {
            const relevantMutations = mutations.filter(mutation => {
                if (mutation.target.classList && 
                    mutation.target.classList.contains(AppConstants.CSS.TOOLTIP)) {
                    return false;
                }
                if (mutation.addedNodes) {
                    for (let node of mutation.addedNodes) {
                        if (node.classList && 
                            node.classList.contains(AppConstants.CSS.TOOLTIP)) {
                            return false;
                        }
                    }
                }
                return true;
            });
            
            if (relevantMutations.length > 0) {
                clearTimeout(tooltipSetupTimeout);
                tooltipSetupTimeout = setTimeout(setupSidebarTooltips, AppConstants.TOOLTIP_SETUP_DELAY);
            }
        });
        
        const sidebar = SidebarUtils.getSidebar();
        if (sidebar) {
            observer.observe(sidebar, {
                childList: true,
                subtree: true,
                attributes: false,
                characterData: false
            });
        }
    }

    /**
     * Initialise le menu au chargement de la page
     */
    function initializeMenu() {
        SidebarUtils.restoreSidebarState();
        setupSidebarTooltips();
        initializeTooltipsObserver();
        
        // Ajouter les listeners sur les liens du menu
        document.addEventListener('DOMContentLoaded', function() {
            const menuLinks = document.querySelectorAll(AppConstants.CSS.MENU_LINK);
            menuLinks.forEach(link => {
                link.addEventListener('click', handleMenuLinkClick);
            });
        });
    }

    /**
     * Toggle le panneau de l'arbre (mobile)
     */
    function toggleTreePanel() {
        const tree = document.getElementById(AppConstants.IDS.TREE_PANEL);
        const iconSpan = document.querySelector(`#${AppConstants.IDS.TREE_TOGGLE} .ui-button-icon-left`);
        const existingHeader = document.getElementById(AppConstants.IDS.TREE_MOBILE_HEADER);

        if (!tree || !iconSpan) return;

        const isVisible = tree.style.display === 'block';

        if (isVisible) {
            hideTreePanel(tree, iconSpan, existingHeader);
        } else {
            showTreePanel(tree, iconSpan, existingHeader);
        }
    }

    /**
     * Affiche le panneau de l'arbre
     */
    function showTreePanel(tree, iconSpan, existingHeader) {
        if (existingHeader) {
            existingHeader.remove();
        }

        Object.assign(tree.style, {
            display: 'block',
            position: 'fixed',
            top: '50px',
            left: '0',
            width: '100vw',
            height: 'calc(100vh - 50px)',
            backgroundColor: '#e8f5e9',
            zIndex: '9999',
            overflowY: 'auto'
        });

        createTreeMobileHeader();
        iconSpan.classList.remove(AppConstants.ICONS.SITEMAP);
        iconSpan.classList.add(AppConstants.ICONS.TIMES);
    }

    /**
     * Masque le panneau de l'arbre
     */
    function hideTreePanel(tree, iconSpan, existingHeader) {
        tree.style.display = 'none';
        if (existingHeader) {
            existingHeader.remove();
        }
        iconSpan.classList.remove(AppConstants.ICONS.TIMES);
        iconSpan.classList.add(AppConstants.ICONS.SITEMAP);
    }

    /**
     * Crée le header mobile pour l'arbre
     */
    function createTreeMobileHeader() {
        const treeHeader = document.createElement('div');
        treeHeader.id = AppConstants.IDS.TREE_MOBILE_HEADER;
        Object.assign(treeHeader.style, {
            position: 'fixed',
            top: '0',
            left: '0',
            width: '100vw',
            height: '50px',
            backgroundColor: 'var(--color-action-main)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: '10000'
        });
        
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
                <i class="${AppConstants.ICONS.ARROW_LEFT}" />
                Retour
            </button>
        `;
        document.body.appendChild(treeHeader);
    }

    // Exposer les fonctions globales nécessaires
    window.toggleSidebar = toggleSidebar;
    window.toggleSidebarCollapse = toggleSidebarCollapse;
    window.closeSidebar = closeSidebar;
    window.toggleTreePanel = toggleTreePanel;

    // Initialisation
    window.addEventListener('resize', handleResponsiveMenu);
    window.addEventListener('load', handleResponsiveMenu);
    document.addEventListener('click', handleOverlayClick);
    
    // Initialiser le menu
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeMenu);
    } else {
        initializeMenu();
    }

})();

