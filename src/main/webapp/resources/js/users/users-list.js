/**
 * Page liste des utilisateurs – paginateur PrimeFaces
 * Remplace "of" par "sur" dans le texte du paginateur (traduction).
 */
(function () {
    'use strict';

    function replacePaginatorText() {
        var elements = document.querySelectorAll('.ui-paginator-current');
        elements.forEach(function (el) {
            if (el.textContent) {
                el.textContent = el.textContent.replace(/\s+of\s+/gi, ' sur ');
            }
        });
    }

    function observePaginatorChanges() {
        var containers = document.querySelectorAll('.ui-paginator');
        containers.forEach(function (container) {
            if (typeof MutationObserver !== 'undefined') {
                var observer = new MutationObserver(function (mutations) {
                    mutations.forEach(function (mutation) {
                        if (mutation.addedNodes.length > 0 || mutation.type === 'childList') {
                            setTimeout(replacePaginatorText, 100);
                        }
                    });
                });
                observer.observe(container, { childList: true, subtree: true });
            }
        });
    }

    function init() {
        replacePaginatorText();
        observePaginatorChanges();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    if (typeof PrimeFaces !== 'undefined') {
        PrimeFaces.ajax.addOnComplete(function () {
            setTimeout(replacePaginatorText, 100);
        });
    }
})();
