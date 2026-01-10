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
