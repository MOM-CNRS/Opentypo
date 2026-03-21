/**
 * Synchronise l'URL du navigateur avec l'élément sélectionné dans la typologie.
 * Permet le partage de liens (ex: /DECOCER) et la navigation par historique.
 */
(function() {
    'use strict';

    function getContextPath() {
        var body = document.body;
        return (body && body.getAttribute('data-context-path')) || '';
    }

    /**
     * Met à jour l'URL du navigateur sans recharger la page.
     * @param {string} code - Code de l'entité (ex: DECOCER) ou vide pour la racine /
     */
    window.updateBrowserUrlAfterNavigation = function(code) {
        var ctx = getContextPath();
        var trimmed = (code != null && code !== '') ? String(code).trim() : '';
        var newPath = trimmed ? (ctx + '/' + encodeURIComponent(trimmed)) : (ctx + '/');
        var currentPath = window.location.pathname;
        if (newPath === currentPath) return;
        try {
            window.history.pushState({ entityCode: trimmed || null }, '', newPath);
        } catch (e) {
            console.warn('urlSync: pushState failed', e);
        }
    };

    /**
     * Récupère le code depuis l'élément sélectionné dans l'arbre (ui-state-highlight).
     */
    window.getEntityCodeFromHighlightedTreeNode = function() {
        var content = document.querySelector('.simple-tree .simple-tree-node-content.ui-state-highlight');
        return content ? (content.getAttribute('data-code') || '') : '';
    };

    /**
     * Écoute popstate pour gérer le bouton Précédent/Suivant du navigateur.
     * Recharge la page pour afficher l'élément correspondant à la nouvelle URL.
     */
    function initPopstateListener() {
        window.addEventListener('popstate', function() {
            var path = window.location.pathname;
            var ctx = getContextPath();
            var relativePath = path.replace(ctx, '') || '/';
            relativePath = relativePath.replace(/^\//, '');
            if (relativePath === 'index.xhtml') relativePath = '';
            if (relativePath.indexOf('/') >= 0 || relativePath.indexOf('.') >= 0) return;
            window.location.reload();
        });
    }

    /**
     * Affiche l'arbre sur mobile lorsqu'on accède via URL (ex: /DECOCER).
     */
    function showTreeOnUrlLoadIfNeeded() {
        if (!window.showTreeOnUrlLoad || window.innerWidth > 768 || typeof toggleTreePanel !== 'function') return;
        var tree = document.getElementById('leftTreePanel');
        if (!tree) return;
        var display = tree.style.display || (window.getComputedStyle && window.getComputedStyle(tree).display);
        if (display === 'none') toggleTreePanel();
    }

    /**
     * Fait défiler l'arbre vers l'élément sélectionné lorsqu'on accède via URL.
     */
    function ensureTreeSelectionVisible() {
        if (!window.showTreeOnUrlLoad) return;
        setTimeout(function() {
            if (typeof applyTreeSelectionHighlight === 'function') applyTreeSelectionHighlight();
        }, 400);
    }

    function init() {
        initPopstateListener();
        showTreeOnUrlLoadIfNeeded();
        ensureTreeSelectionVisible();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        setTimeout(init, 100);
    }
})();
