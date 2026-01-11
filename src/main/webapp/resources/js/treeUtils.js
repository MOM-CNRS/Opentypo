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
 * Restaure la sélection de l'arbre après une mise à jour AJAX
 * Cette fonction force la sélection du nœud qui est dans treeBean.selectedNode
 */
function restoreTreeSelection() {
    // Attendre que le DOM soit complètement mis à jour
    setTimeout(function() {
        if (typeof PF !== 'undefined' && PF('treeWidget')) {
            var tree = PF('treeWidget');
            
            // Essayer de récupérer le nœud sélectionné depuis le widget
            // PrimeFaces devrait préserver selectedNode après la mise à jour grâce à selection="#{treeBean.selectedNode}"
            var selectedNodes = tree.getSelectedNodes();
            var nodeToSelect = null;
            
            if (selectedNodes && selectedNodes.length > 0) {
                nodeToSelect = selectedNodes[0];
            }
            
            // Si le widget n'a pas de nœud sélectionné, parcourir tous les nœuds pour trouver celui qui correspond
            // On cherche un nœud qui a la classe ui-state-highlight ou ui-treenode-selected
            if (!nodeToSelect) {
                var allTreeNodes = document.querySelectorAll('.modern-tree .ui-treenode');
                for (var i = 0; i < allTreeNodes.length; i++) {
                    var treeNodeElement = allTreeNodes[i];
                    var content = treeNodeElement.querySelector('.ui-treenode-content');
                    if (content && (content.classList.contains('ui-state-highlight') || treeNodeElement.classList.contains('ui-treenode-selected'))) {
                        var nodeKey = treeNodeElement.getAttribute('data-nodekey');
                        if (nodeKey) {
                            try {
                                nodeToSelect = tree.getNodeByKey(nodeKey);
                                if (nodeToSelect) {
                                    break;
                                }
                            } catch (e) {
                                // Continuer la recherche
                            }
                        }
                    }
                }
            }
            
            // Si on a trouvé un nœud, forcer sa sélection
            if (nodeToSelect) {
                // Forcer la sélection via PrimeFaces
                if (typeof tree.selectNode === 'function') {
                    tree.selectNode(nodeToSelect);
                }
                
                // Attendre un peu et forcer l'application des classes CSS
                setTimeout(function() {
                    // Trouver l'élément DOM correspondant
                    var nodeKey = null;
                    try {
                        // Essayer d'obtenir le nodeKey du nœud
                        if (typeof tree.getNodeKey === 'function') {
                            nodeKey = tree.getNodeKey(nodeToSelect);
                        }
                    } catch (e) {
                        // Si on ne peut pas obtenir le nodeKey, chercher dans le DOM
                    }
                    
                    var selectedNodeElement = null;
                    
                    if (nodeKey) {
                        selectedNodeElement = document.querySelector('.modern-tree .ui-treenode[data-nodekey="' + nodeKey + '"]');
                    }
                    
                    // Si on n'a pas trouvé par nodeKey, chercher par les classes
                    if (!selectedNodeElement) {
                        selectedNodeElement = document.querySelector('.modern-tree .ui-treenode.ui-treenode-selected');
                    }
                    
                    // Si toujours pas trouvé, parcourir tous les nœuds
                    if (!selectedNodeElement) {
                        var allNodes = document.querySelectorAll('.modern-tree .ui-treenode');
                        for (var i = 0; i < allNodes.length; i++) {
                            var nodeElement = allNodes[i];
                            var nodeKeyAttr = nodeElement.getAttribute('data-nodekey');
                            if (nodeKeyAttr) {
                                try {
                                    var node = tree.getNodeByKey(nodeKeyAttr);
                                    if (node === nodeToSelect) {
                                        selectedNodeElement = nodeElement;
                                        break;
                                    }
                                } catch (e) {
                                    // Continuer
                                }
                            }
                        }
                    }
                    
                    if (selectedNodeElement) {
                        // Retirer la sélection de tous les autres nœuds
                        document.querySelectorAll('.modern-tree .ui-treenode-content.ui-state-highlight').forEach(function(el) {
                            el.classList.remove('ui-state-highlight');
                        });
                        document.querySelectorAll('.modern-tree .ui-treenode.ui-treenode-selected').forEach(function(el) {
                            el.classList.remove('ui-treenode-selected');
                        });
                        
                        // Appliquer la sélection au bon nœud
                        var content = selectedNodeElement.querySelector('.ui-treenode-content');
                        if (content) {
                            content.classList.add('ui-state-highlight');
                            selectedNodeElement.classList.add('ui-treenode-selected');
                        }
                        
                        // Étendre automatiquement le nœud sélectionné s'il a des enfants
                        var nodeKeyAttr = selectedNodeElement.getAttribute('data-nodekey');
                        if (nodeKeyAttr) {
                            var toggler = selectedNodeElement.querySelector('.ui-tree-toggler');
                            var hasChildren = toggler !== null;
                            
                            var childrenContainer = selectedNodeElement.querySelector('.ui-treenode-children');
                            if (childrenContainer && childrenContainer.children.length > 0) {
                                hasChildren = true;
                            }
                            
                            if (!selectedNodeElement.classList.contains('ui-treenode-expanded') && hasChildren) {
                                var node = tree.getNodeByKey(nodeKeyAttr);
                                if (node && typeof tree.expandNode === 'function') {
                                    tree.expandNode(node);
                                }
                            }
                        }
                    }
                }, 200);
            }
        }
    }, 400);
}

/**
 * Rafraîchit le widget tree PrimeFaces pour afficher les nouveaux enfants chargés
 * et étend automatiquement le nœud sélectionné s'il a des enfants
 */
function refreshTreeWidget() {
    setTimeout(function() {
        if (typeof PF !== 'undefined' && PF('treeWidget')) {
            var tree = PF('treeWidget');
            
            // Sauvegarder l'état actuel (nœud sélectionné et nœuds dépliés)
            var selectedNodes = tree.getSelectedNodes();
            var expandedNodes = [];
            
            // Collecter tous les nœuds dépliés
            var allNodes = document.querySelectorAll('.modern-tree .ui-treenode');
            allNodes.forEach(function(node) {
                if (node.classList.contains('ui-treenode-expanded')) {
                    var nodeKey = node.getAttribute('data-nodekey');
                    if (nodeKey) {
                        expandedNodes.push(nodeKey);
                    }
                }
            });
            
            // Forcer la mise à jour du widget tree
            if (tree && typeof tree.refresh === 'function') {
                try {
                    tree.refresh();
                } catch (e) {
                    console.log('Tree refresh method not available, using alternative');
                }
            }
            
            // Restaurer la sélection et étendre le nœud sélectionné s'il a des enfants
            if (selectedNodes && selectedNodes.length > 0) {
                setTimeout(function() {
                    var nodeToSelect = selectedNodes[0];
                    if (nodeToSelect && typeof tree.selectNode === 'function') {
                        tree.selectNode(nodeToSelect);
                        
                        // Étendre automatiquement le nœud sélectionné s'il a des enfants
                        setTimeout(function() {
                            var selectedNodeElement = document.querySelector('.modern-tree .ui-treenode.ui-treenode-selected');
                            if (selectedNodeElement) {
                                var nodeKey = selectedNodeElement.getAttribute('data-nodekey');
                                if (nodeKey) {
                                    // Vérifier si le nœud a un toggler (indique qu'il peut avoir des enfants)
                                    var toggler = selectedNodeElement.querySelector('.ui-tree-toggler');
                                    var hasChildren = toggler !== null;
                                    
                                    // Vérifier aussi s'il y a déjà des enfants visibles
                                    var childrenContainer = selectedNodeElement.querySelector('.ui-treenode-children');
                                    if (childrenContainer && childrenContainer.children.length > 0) {
                                        hasChildren = true;
                                    }
                                    
                                    // Si le nœud n'est pas déjà déplié et qu'il a un toggler (donc peut avoir des enfants), l'étendre
                                    if (!selectedNodeElement.classList.contains('ui-treenode-expanded') && hasChildren) {
                                        var node = tree.getNodeByKey(nodeKey);
                                        if (node && typeof tree.expandNode === 'function') {
                                            tree.expandNode(node);
                                        }
                                    }
                                }
                            }
                        }, 100);
                    }
                }, 300);
            }
            
            // Restaurer les nœuds dépliés
            setTimeout(function() {
                expandedNodes.forEach(function(nodeKey) {
                    var nodeElement = document.querySelector('.modern-tree .ui-treenode[data-nodekey="' + nodeKey + '"]');
                    if (nodeElement && !nodeElement.classList.contains('ui-treenode-expanded')) {
                        var node = tree.getNodeByKey(nodeKey);
                        if (node && typeof tree.expandNode === 'function') {
                            tree.expandNode(node);
                        }
                    }
                });
            }, 400);
        }
    }, 200);
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
