/**
 * Utilitaires pour la gestion de la sidebar
 */
const SidebarUtils = {
    /**
     * Obtient l'élément sidebar
     */
    getSidebar() {
        return document.getElementById(AppConstants.IDS.SIDEBAR);
    },
    
    /**
     * Obtient le bouton toggle de la sidebar
     */
    getToggleButton() {
        return document.querySelector(
            `.${AppConstants.CSS.SIDEBAR_TOGGLE} .ui-button-icon-left`
        ) || document.querySelector(
            `.${AppConstants.CSS.SIDEBAR_TOGGLE} .ui-icon`
        ) || document.querySelector(
            `.${AppConstants.CSS.SIDEBAR_TOGGLE} i`
        );
    },
    
    /**
     * Met à jour l'icône du bouton toggle
     */
    updateToggleIcon(isCollapsed) {
        const toggleButton = this.getToggleButton();
        if (!toggleButton) return;
        
        toggleButton.classList.remove(
            AppConstants.ICONS.ANGLE_LEFT,
            AppConstants.ICONS.ANGLE_RIGHT,
            'pi-angle-double-left',
            'pi-angle-double-right'
        );
        
        toggleButton.classList.add(
            isCollapsed ? AppConstants.ICONS.ANGLE_RIGHT : AppConstants.ICONS.ANGLE_LEFT
        );
    },
    
    /**
     * Sauvegarde l'état de la sidebar dans localStorage
     */
    saveSidebarState(isCollapsed) {
        localStorage.setItem(
            AppConstants.STORAGE.SIDEBAR_COLLAPSED,
            isCollapsed.toString()
        );
    },
    
    /**
     * Restaure l'état de la sidebar depuis localStorage
     */
    restoreSidebarState() {
        if (window.innerWidth <= AppConstants.MOBILE_BREAKPOINT) {
            return;
        }
        
        const sidebar = this.getSidebar();
        if (!sidebar) return;
        
        const savedState = localStorage.getItem(AppConstants.STORAGE.SIDEBAR_COLLAPSED);
        const isCollapsed = savedState === 'true' || savedState === null; // Par défaut collapsed
        
        if (isCollapsed) {
            sidebar.classList.add(AppConstants.CSS.SIDEBAR_COLLAPSED);
        } else {
            sidebar.classList.remove(AppConstants.CSS.SIDEBAR_COLLAPSED);
        }
        
        this.updateToggleIcon(isCollapsed);
    }
};

