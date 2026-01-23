        // Remplacer "of" par "sur" dans le paginateur
        function replacePaginatorText() {
            const paginatorCurrents = document.querySelectorAll('.ui-paginator-current');
            paginatorCurrents.forEach(function(element) {
                if (element.textContent) {
                    element.textContent = element.textContent.replace(/\s+of\s+/gi, ' sur ');
                }
            });
        }

        // Exécuter au chargement de la page
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', function() {
                replacePaginatorText();
                // Observer les changements du DOM pour les mises à jour dynamiques
                observePaginatorChanges();
            });
        } else {
            replacePaginatorText();
            observePaginatorChanges();
        }

        // Observer les changements dans le paginateur (pour les mises à jour AJAX)
        function observePaginatorChanges() {
            const paginatorContainers = document.querySelectorAll('.ui-paginator');
            paginatorContainers.forEach(function(container) {
                if (typeof MutationObserver !== 'undefined') {
                    const observer = new MutationObserver(function(mutations) {
                        mutations.forEach(function(mutation) {
                            if (mutation.addedNodes.length > 0 || mutation.type === 'childList') {
                                setTimeout(replacePaginatorText, 100);
                            }
                        });
                    });
                    observer.observe(container, { childList: true, subtree: true });
                }
            });
        }

        // Écouter les événements AJAX de PrimeFaces
        if (typeof PrimeFaces !== 'undefined') {
            PrimeFaces.ajax.addOnComplete(function() {
                setTimeout(replacePaginatorText, 100);
            });
        }
