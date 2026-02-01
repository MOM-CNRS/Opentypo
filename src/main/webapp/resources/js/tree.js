/**
 * Arbre personnalisé (HTML/JS) : même comportement que p:tree (expand/collapse, sélection AJAX).
 */
(function() {
    'use strict';

    function findTreeContainer() {
        return document.getElementById('simpleTree') || document.querySelector('.simple-tree');
    }

    function toggleNodeExpanded(li) {
        if (!li || !li.classList.contains('simple-tree-node')) return;
        li.classList.toggle('expanded');
    }

    function clearSelectionHighlight() {
        var tree = findTreeContainer();
        if (!tree) return;
        tree.querySelectorAll('.simple-tree-node-content.ui-state-highlight').forEach(function(el) {
            el.classList.remove('ui-state-highlight');
        });
    }

    function setSelectionHighlight(entityId) {
        var tree = findTreeContainer();
        if (!tree || !entityId) return;
        var content = tree.querySelector('.simple-tree-node-content[data-entity-id="' + entityId + '"]');
        if (content) content.classList.add('ui-state-highlight');
    }

    window.applyTreeSelectionHighlight = function() {
        var tree = findTreeContainer();
        if (!tree) return;
        var sel = tree.querySelector('.simple-tree-node-content.ui-state-highlight');
        if (sel) sel.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    };

    /**
     * Injecte le fragment HTML des enfants (ul) reçu du serveur dans le nœud correspondant de l'arbre,
     * sans remplacer tout l'arbre, pour préserver l'état déplié/empilé des autres nœuds.
     * @param {string} entityId - ID de l'entité dont on vient de charger les enfants
     */
    window.injectTreeChildrenFragment = function(entityId) {
        if (!entityId) return;
        var fragmentPanel = document.getElementById('treeExpandForm:treeLoadChildrenFragment') ||
            document.querySelector('[id$="treeLoadChildrenFragment"]');
        var ul = fragmentPanel ? fragmentPanel.querySelector('ul.simple-tree-children') : null;
        var tree = findTreeContainer();
        if (!tree) return;
        var targetLi = tree.querySelector('li.simple-tree-node[data-entity-id="' + entityId + '"]');
        if (!targetLi) return;
        if (ul) {
            targetLi.appendChild(ul);
        }
        /* Forcer le navigateur à appliquer max-height: 0 (reflow), puis ajouter expanded pour que l'animation joue à tous les niveaux. */
        void targetLi.offsetHeight;
        requestAnimationFrame(function() {
            requestAnimationFrame(function() {
                targetLi.classList.add('expanded');
                if (ul && ul.querySelectorAll('li.simple-tree-node').length === 0) {
                    targetLi.classList.add('simple-tree-node-empty-children');
                }
            });
        });
    };

    function handleTreeClick(ev) {
        var toggler = ev.target.closest('[data-tree-toggler]');
        if (toggler && findTreeContainer() && findTreeContainer().contains(toggler)) {
            ev.preventDefault();
            ev.stopPropagation();
            var li = toggler.closest('li.simple-tree-node');
            if (!li) return;
            var entityId = li.getAttribute('data-entity-id');
            var isAlreadyExpanded = li.classList.contains('expanded');
            var childrenUl = li.querySelector('.simple-tree-children');
            var hasChildrenInDom = childrenUl && childrenUl.querySelectorAll('li.simple-tree-node').length > 0;
            var canHaveChildren = li.getAttribute('data-can-have-children') === 'true';
            /* Ne charger les enfants que si le nœud n'est pas déjà déplié (évite de recharger au clic) */
            if (!isAlreadyExpanded && !hasChildrenInDom && canHaveChildren && entityId) {
                var form = document.getElementById('treeExpandForm') || document.querySelector('[id$="treeExpandForm"]');
                if (form) {
                    var input = form.querySelector('input[id$="treeExpandEntityId"]') || form.querySelector('input[name*="treeExpandEntityId"]');
                    var btn = form.querySelector('[id$="loadTreeChildrenBtn"]');
                    if (input && btn) {
                        input.value = entityId;
                        btn.click();
                    } else {
                        toggleNodeExpanded(li);
                    }
                } else {
                    toggleNodeExpanded(li);
                }
            } else {
                toggleNodeExpanded(li);
            }
            return;
        }

        var content = ev.target.closest('.simple-tree .simple-tree-node-content');
        if (!content) return;

        var li = content.closest('li.simple-tree-node');
        var entityId = li ? li.getAttribute('data-entity-id') : content.getAttribute('data-entity-id');
        if (!entityId) return;

        ev.preventDefault();
        ev.stopPropagation();

        clearSelectionHighlight();
        setSelectionHighlight(entityId);

        /* Déclencher la soumission : formulaire + champ caché + bouton (fiable même sans widget PrimeFaces) */
        var form = document.getElementById('treeSelectForm') || document.querySelector('[id$="treeSelectForm"]');
        if (form) {
            var input = form.querySelector('input[id$="treeEntityId"]') || form.querySelector('input[name*="treeEntityId"]');
            var btn = form.querySelector('[id$="selectTreeEntityBtn"]') || form.querySelector('button[type="submit"]');
            if (input && btn) {
                input.value = entityId;
                btn.click();
                return;
            }
        }
        /* Fallback : remoteCommand PrimeFaces */
        if (typeof PF !== 'undefined') {
            var cmd = PF('selectTreeEntity');
            if (cmd && typeof cmd.call === 'function') {
                cmd.call([{ name: 'entityId', value: entityId }]);
            }
        }
    }

    function init() {
        document.removeEventListener('click', handleTreeClick, true);
        document.addEventListener('click', handleTreeClick, true);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
