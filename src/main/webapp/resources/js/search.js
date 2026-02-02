/**
 * Scripts pour la page de recherche (search.xhtml).
 * - Recherche au clavier (Entrée)
 * - Affichage du panneau de résultats après une recherche
 * - Options avancées, panneau latéral, arbre
 */
(function() {
    'use strict';

    function toggleAdvancedSearch() {
        var section = document.getElementById('advanced-search-options');
        if (section) {
            section.classList.toggle('advanced-search-visible');
        }
    }

    function updateToggleButtonIcon() {
        var panel = document.querySelector('[id$="panelRight"]');
        var button = document.querySelector('[id$="toggleResultsPanelBtn"]');

        if (!panel || !button) return;

        var iconSpan = button.querySelector('.ui-button-icon-left, .ui-button-icon');
        if (!iconSpan) return;

        var isVisible = panel.classList.contains('visible');

        if (isVisible) {
            iconSpan.classList.remove('pi-chevron-right');
            iconSpan.classList.add('pi-chevron-left');
        } else {
            iconSpan.classList.remove('pi-chevron-left');
            iconSpan.classList.add('pi-chevron-right');
        }
    }

    function showResultsPanel() {
        var panel = document.querySelector('[id$="panelRight"]');

        if (!panel) return;

        panel.classList.add('visible');
        updateToggleButtonIcon();
    }

    function toggleRightPanel() {
        var panel = document.querySelector('[id$="panelRight"]');

        if (!panel) return;

        panel.classList.toggle('visible');
        updateToggleButtonIcon();
    }

    /**
     * À appeler après une recherche (bouton ou Entrée) pour afficher le panneau des résultats.
     */
    function onSearchComplete() {
        showResultsPanel();
    }

    /**
     * Déclenche la recherche en simulant un clic sur le bouton de recherche.
     */
    function triggerSearch() {
        var form = document.querySelector('form[id$="searchForm"]');
        if (!form) return;
        var btn = form.querySelector('.search-button-compact');
        if (btn) btn.click();
    }

    /**
     * Gestion de la touche Entrée dans le champ de recherche.
     */
    function initEnterKeySearch() {
        document.addEventListener('keydown', function(e) {
            if (e.key !== 'Enter') return;
            var target = e.target;
            if (!target || !target.classList || !target.classList.contains('search-input-compact')) return;

            e.preventDefault();
            triggerSearch();
        });
    }

    function styleCollectionMenuItems() {
        var menuItems = document.querySelectorAll('.search-compact-select .ui-selectonemenu-items .ui-selectonemenu-item');
        menuItems.forEach(function(item) {
            var label = item.querySelector('.ui-selectonemenu-item-label');
            if (label) {
                var text = label.textContent || label.innerText;
                if (text.trim().indexOf('📁') === 0) {
                    item.classList.add('collection-menu-item');
                    item.classList.remove('reference-menu-item');
                } else if (text.indexOf('📖') !== -1) {
                    var hasIndentation = text.indexOf(' ') === 0 || text.indexOf('\u00A0') === 0;
                    if (hasIndentation) {
                        item.classList.add('reference-menu-item');
                        item.classList.remove('collection-menu-item');
                    }
                }
            }
        });
    }

    function initCollectionMenuStyles() {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', styleCollectionMenuItems);
        } else {
            styleCollectionMenuItems();
        }

        if (typeof MutationObserver !== 'undefined') {
            var observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(mutation) {
                    if (mutation.addedNodes.length > 0) {
                        setTimeout(styleCollectionMenuItems, 100);
                    }
                });
            });
            var menuContainer = document.querySelector('.search-compact-select');
            if (menuContainer) {
                observer.observe(menuContainer, { childList: true, subtree: true });
            }
        }

        document.addEventListener('click', function(e) {
            if (e.target) {
                var isTrigger = e.target.classList.contains('ui-selectonemenu-trigger') ||
                    (e.target.closest && e.target.closest('.ui-selectonemenu-trigger'));
                if (isTrigger) {
                    var selectMenu = e.target.closest ? e.target.closest('.search-compact-select') : null;
                    if (selectMenu) {
                        setTimeout(styleCollectionMenuItems, 200);
                    }
                }
            }
        });

        if (typeof jQuery !== 'undefined') {
            jQuery(document).on('click', '.search-compact-select .ui-selectonemenu-trigger', function() {
                setTimeout(styleCollectionMenuItems, 200);
            });
        }
    }

    function resetTreeOnResize() {
        var tree = document.getElementById('leftTreePanel');
        var iconSpan = document.querySelector('#treeToggleButton .ui-button-icon-left');

        if (!tree || !iconSpan) return;

        if (window.innerWidth >= 769) {
            tree.style.display = '';
            tree.style.position = '';
            tree.style.top = '';
            tree.style.left = '';
            tree.style.width = '';
            tree.style.height = '';
            tree.style.backgroundColor = '';
            tree.style.zIndex = '';
            tree.style.overflowY = '';

            iconSpan.classList.remove('pi-times');
            iconSpan.classList.add('pi-sitemap');
        }
    }

    function init() {
        initEnterKeySearch();
        initCollectionMenuStyles();

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', function() {
                updateToggleButtonIcon();
                resetTreeOnResize();
            });
        } else {
            setTimeout(updateToggleButtonIcon, 100);
            resetTreeOnResize();
        }

        window.addEventListener('resize', resetTreeOnResize);
    }

    init();

    // API globale pour les appels depuis le XHTML (oncomplete, onclick, etc.)
    window.SearchPage = {
        toggleAdvancedSearch: toggleAdvancedSearch,
        updateToggleButtonIcon: updateToggleButtonIcon,
        showResultsPanel: showResultsPanel,
        toggleRightPanel: toggleRightPanel,
        onSearchComplete: onSearchComplete,
        triggerSearch: triggerSearch,
        resetTreeOnResize: resetTreeOnResize
    };
})();
