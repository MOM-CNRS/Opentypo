/**
 * Fonction pour sélectionner le nœud dans l'arbre après mise à jour
 */
function selectTreeNodeAfterUpdate() {
    setTimeout(function() {
        if (typeof PF !== 'undefined' && PF('treeWidget')) {
            var tree = PF('treeWidget');
            var selectedNodes = tree.getSelectedNodes();
            if (selectedNodes && selectedNodes.length > 0) {
                tree.selectNode(selectedNodes[0]);
            }
        }
    }, 300);
}

/**
 * Force l'affichage du toggler pour la racine de l'arbre
 * Cette fonction est appelée après l'initialisation de l'arbre pour s'assurer
 * que le toggler est visible même si PrimeFaces ne l'a pas généré
 */
function ensureRootTogglerVisible() {
    setTimeout(function() {
        // Trouver le premier nœud (racine) dans l'arbre
        var treeContainer = document.querySelector('.modern-tree .ui-tree-container');
        if (!treeContainer) {
            return;
        }
        
        var rootNode = treeContainer.querySelector(':scope > .ui-treenode');
        if (!rootNode) {
            return;
        }
        
        var rootContent = rootNode.querySelector('.ui-treenode-content');
        if (!rootContent) {
            return;
        }
        
        // Vérifier si le toggler existe
        var toggler = rootContent.querySelector('.ui-tree-toggler');
        
        if (!toggler) {
            // Si le toggler n'existe pas, le créer
            toggler = document.createElement('span');
            toggler.className = 'ui-tree-toggler ui-icon ui-icon-triangle-1-e';
            toggler.setAttribute('role', 'button');
            toggler.setAttribute('aria-label', 'Expand');
            
            // Insérer le toggler au début du contenu
            var firstChild = rootContent.firstChild;
            if (firstChild) {
                rootContent.insertBefore(toggler, firstChild);
            } else {
                rootContent.appendChild(toggler);
            }
            
            // Ajouter l'événement de clic pour déclencher l'expansion
            toggler.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                
                // Déclencher l'événement expand via PrimeFaces
                if (typeof PF !== 'undefined' && PF('treeWidget')) {
                    var tree = PF('treeWidget');
                    var rootNodeData = rootNode.getAttribute('data-nodekey') || '0';
                    tree.expandNode(rootNode);
                }
            });
        }
        
        // Forcer la visibilité du toggler
        toggler.style.display = 'flex';
        toggler.style.visibility = 'visible';
        toggler.style.opacity = '1';
        toggler.style.pointerEvents = 'auto';
        
        // Retirer la classe leaf si elle existe pour que PrimeFaces considère le nœud comme expandable
        rootNode.classList.remove('ui-treenode-leaf');
    }, 100);
}

// Appeler la fonction après le chargement de la page et après chaque mise à jour de l'arbre
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', ensureRootTogglerVisible);
} else {
    ensureRootTogglerVisible();
}

// Observer les changements dans l'arbre pour réappliquer la fonction si nécessaire
if (typeof MutationObserver !== 'undefined') {
    var observer = new MutationObserver(function(mutations) {
        var shouldReapply = false;
        mutations.forEach(function(mutation) {
            if (mutation.type === 'childList' || mutation.type === 'attributes') {
                var target = mutation.target;
                if (target.classList && target.classList.contains('ui-tree-container')) {
                    shouldReapply = true;
                }
            }
        });
        if (shouldReapply) {
            setTimeout(ensureRootTogglerVisible, 200);
        }
    });
    
    // Observer les changements dans le conteneur de l'arbre
    setTimeout(function() {
        var treeContainer = document.querySelector('.modern-tree .ui-tree-container');
        if (treeContainer) {
            observer.observe(treeContainer, {
                childList: true,
                subtree: true,
                attributes: true,
                attributeFilter: ['class']
            });
        }
    }, 500);
}
