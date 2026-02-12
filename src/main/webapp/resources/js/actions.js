/**
 * Délégation des actions via data-action / data-dialog pour éviter le JS inline dans les XHTML.
 * Utiliser data-action="..." et éventuellement data-dialog="..." sur les boutons/liens.
 */
(function() {
    'use strict';

    function handleClick(e) {
        var el = e.target.closest && e.target.closest('[data-action]');
        if (!el) return;

        var action = el.getAttribute('data-action');
        if (!action) return;

        switch (action) {
            case 'toggle-sidebar':
                e.preventDefault();
                e.stopPropagation();
                if (typeof window.toggleSidebar === 'function') {
                    window.toggleSidebar();
                }
                break;

            case 'show-login-dialog':
                e.preventDefault();
                e.stopPropagation();
                if (typeof window.showLoginDialog === 'function') {
                    window.showLoginDialog();
                }
                break;

            case 'show-dialog':
                e.preventDefault();
                e.stopPropagation();
                var widgetVar = el.getAttribute('data-dialog');
                if (widgetVar && typeof PF !== 'undefined') {
                    var widget = PF(widgetVar);
                    if (widget && typeof widget.show === 'function') {
                        widget.show();
                    }
                }
                break;

            case 'hide-dialog':
                e.preventDefault();
                e.stopPropagation();
                var hideWidgetVar = el.getAttribute('data-dialog');
                if (hideWidgetVar && typeof PF !== 'undefined') {
                    var hideWidget = PF(hideWidgetVar);
                    if (hideWidget && typeof hideWidget.hide === 'function') {
                        hideWidget.hide();
                    }
                }
                break;

            case 'toggle-tree-panel':
                e.preventDefault();
                e.stopPropagation();
                if (typeof window.toggleTreePanel === 'function') {
                    window.toggleTreePanel();
                }
                break;

            case 'search-toggle-advanced':
                e.preventDefault();
                e.stopPropagation();
                if (typeof window.SearchPage !== 'undefined' && typeof window.SearchPage.toggleAdvancedSearch === 'function') {
                    window.SearchPage.toggleAdvancedSearch();
                }
                break;

            case 'search-toggle-right-panel':
                e.preventDefault();
                e.stopPropagation();
                if (typeof window.SearchPage !== 'undefined' && typeof window.SearchPage.toggleRightPanel === 'function') {
                    window.SearchPage.toggleRightPanel();
                }
                break;

            case 'show-fullscreen-image':
                e.preventDefault();
                e.stopPropagation();
                var url = el.getAttribute('data-image-url');
                if (url && typeof window.showFullscreenImage === 'function') {
                    window.showFullscreenImage(url);
                }
                break;

            case 'hide-fullscreen-image':
                e.preventDefault();
                e.stopPropagation();
                if (typeof window.hideFullscreenImage === 'function') {
                    window.hideFullscreenImage();
                }
                break;

            case 'stop-propagation':
                e.stopPropagation();
                break;

            default:
                break;
        }
    }

    document.addEventListener('click', handleClick, true);
})();
