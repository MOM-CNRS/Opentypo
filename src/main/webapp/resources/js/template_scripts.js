// Définir showLoginDialog immédiatement pour éviter les erreurs de référence
// Cette fonction doit être disponible avant que le header ne soit rendu
(function() {
    'use strict';
    if (typeof window.showLoginDialog === 'undefined') {
        window.showLoginDialog = function() {
            var attempts = 0;
            var maxAttempts = 30;
            
            var checkDialog = function() {
                if (typeof PF !== 'undefined') {
                    var dialog = PF('loginDialog');
                    if (dialog) {
                        dialog.show();
                    } else if (attempts < maxAttempts) {
                        attempts++;
                        setTimeout(checkDialog, 100);
                    } else {
                        console.warn('Dialog loginDialog not found. Make sure the login dialog is included in the template.');
                    }
                } else if (attempts < maxAttempts) {
                    attempts++;
                    setTimeout(checkDialog, 100);
                } else {
                    console.warn('PrimeFaces (PF) is not available.');
                }
            };
            
            checkDialog();
        };
    }
})();

// Fonction pour mettre à jour l'item actif du menu
function updateActiveMenuItem() {
    const currentPath = (window.location.pathname || '').toLowerCase();
    const menuItems = document.querySelectorAll('.sidebar-menu-item');
    let hasActiveItem = false;
    const isAccueilPath = currentPath === '/' || currentPath === '/index.xhtml' || currentPath.endsWith('/index.xhtml') || currentPath === '';
    const isCandidatsPath = currentPath.indexOf('/candidats') !== -1;
    const isUsersPath = currentPath.indexOf('/users') !== -1;

    // Retirer la classe active de tous les items
    menuItems.forEach(function(menuItem) {
        menuItem.classList.remove('active');
    });

    menuItems.forEach(function(item) {
        const menuItemType = (item.getAttribute('data-menu-item') || '').toLowerCase();
        const itemText = (item.querySelector('.sidebar-menu-item-text')?.textContent?.trim() || '').toLowerCase();

        let shouldBeActive = false;

        // Utiliser l'attribut data-menu-item pour une détection précise
        if (menuItemType === 'accueil') {
            shouldBeActive = isAccueilPath && !isCandidatsPath && !isUsersPath;
        }
        else if (menuItemType === 'candidats') {
            shouldBeActive = isCandidatsPath;
        }
        else if (menuItemType === 'users') {
            shouldBeActive = isUsersPath;
        }
        // Fallback : détection basée sur le texte si data-menu-item n'est pas disponible
        else {
            if (itemText.indexOf('accueil') !== -1) {
                shouldBeActive = isAccueilPath && !isCandidatsPath && !isUsersPath;
            }
            else if (itemText.indexOf('brouillons') !== -1 || itemText.indexOf('candidats') !== -1) {
                shouldBeActive = isCandidatsPath;
            }
            else if (itemText.indexOf('utilisateur') !== -1) {
                shouldBeActive = isUsersPath;
            }
        }

        if (shouldBeActive) {
            item.classList.add('active');
            hasActiveItem = true;
        }
    });

    // Si aucun item n'est actif et qu'on est sur la page d'accueil, sélectionner Accueil
    if (!hasActiveItem && isAccueilPath) {
        const accueilItem = document.querySelector('[data-menu-item="accueil"]');
        if (accueilItem) {
            accueilItem.classList.add('active');
        }
    }
}

// Fonction pour gérer les clics sur les items du menu
function setupMenuClickHandlers() {
    const menuItems = document.querySelectorAll('.sidebar-menu-item');
    menuItems.forEach(function(item) {
        const link = item.querySelector('a') || item;
        // Retirer les anciens listeners pour éviter les doublons
        const newLink = link.cloneNode(true);
        link.parentNode.replaceChild(newLink, link);

        // Ajouter le nouveau listener
        newLink.addEventListener('click', function() {
            // Retirer active de tous les items
            menuItems.forEach(function(menuItem) {
                menuItem.classList.remove('active');
            });
            // Ajouter active à l'item cliqué
            item.classList.add('active');
        });
    });
}

document.addEventListener('DOMContentLoaded', function() {
    updateActiveMenuItem();
    setupMenuClickHandlers();
    setupSidebarMenuTooltips();
});

// Mettre à jour après les mises à jour AJAX
if (typeof PrimeFaces !== 'undefined') {
    PrimeFaces.ajax.addOnUpdate(function(data) {
        setTimeout(function() {
            updateActiveMenuItem();
        }, 100);
    });

    // Écouter aussi les événements de navigation JSF
    PrimeFaces.ajax.addOnComplete(function(xhr, status, args) {
        setTimeout(function() {
            updateActiveMenuItem();
        }, 150);
    });
}

// Mettre à jour lors des changements d'URL (navigation)
window.addEventListener('popstate', function() {
    setTimeout(function() {
        updateActiveMenuItem();
    }, 100);
});

// Observer les changements dans l'URL (pour JSF navigation)
let lastUrl = window.location.href;
setInterval(function() {
    const currentUrl = window.location.href;
    if (currentUrl !== lastUrl) {
        lastUrl = currentUrl;
        setTimeout(function() {
            updateActiveMenuItem();
        }, 100);
    }
}, 500);

// Exposer la fonction globalement pour pouvoir l'appeler manuellement si nécessaire
window.updateActiveMenuItem = updateActiveMenuItem;

function setupSidebarMenuTooltips() {
    const menuItems = document.querySelectorAll('.sidebar-menu-item');
    menuItems.forEach(function(item) {
        const tooltip = item.querySelector('.sidebar-menu-item-tooltip');
        const textElement = item.querySelector('.sidebar-menu-item-text');

        if (!tooltip && textElement) {
            const tooltipText = textElement.textContent.trim();
            if (tooltipText) {
                const tooltipSpan = document.createElement('span');
                tooltipSpan.className = 'sidebar-menu-item-tooltip';
                tooltipSpan.textContent = tooltipText;
                item.querySelector('.sidebar-menu-item-content').appendChild(tooltipSpan);
            }
        }
    });
}

// Fonction pour afficher uniquement le conteneur des cartes
// et masquer les panneaux de contenu, tout en gardant le tree panel visible
window.showOnlyCardsContainer = function() {
    // Masquer les panneaux de contenu
    var contentPanels = document.getElementById('contentPanels');
    if (contentPanels) {
        contentPanels.style.display = 'none';
    }

    // Afficher le conteneur des cartes
    var cardsContainer = document.getElementById('cardsContainer');
    if (cardsContainer) {
        cardsContainer.style.display = 'block';
    }

    // S'assurer que le tree panel reste visible
    var leftTreePanel = document.getElementById('leftTreePanel');
    if (leftTreePanel) {
        leftTreePanel.style.display = 'block';
    }
};

// Easing easeOutQuart : démarrage vif puis ralentissement progressif (effet dépilement/remontée fluide)
function easeOutQuart(t) {
    return 1 - Math.pow(1 - t, 4);
}

// Scroll animé vers le haut pour un élément (fenêtre ou conteneur) – effet dépilement progressif
function scrollElementToTop(element, duration) {
    duration = duration || 1200;
    var isWindow = element === window || element === document.documentElement;
    var getScrollTop = function() {
        return isWindow ? (window.scrollY || document.documentElement.scrollTop) : element.scrollTop;
    };
    var setScrollTop = function(v) {
        if (isWindow) {
            window.scrollTo(0, v);
        } else if (element) {
            element.scrollTop = v;
        }
    };
    var start = getScrollTop();
    if (start <= 0) return;
    var startTime = null;
    function animateScroll(currentTime) {
        if (startTime === null) startTime = currentTime;
        var elapsed = currentTime - startTime;
        var progress = Math.min(elapsed / duration, 1);
        var eased = easeOutQuart(progress);
        setScrollTop(start * (1 - eased));
        if (progress < 1) requestAnimationFrame(animateScroll);
    }
    requestAnimationFrame(animateScroll);
}

// Scroll vers le haut : fenêtre + conteneurs internes avec effet dépilement (glissement fluide)
function scrollToTopAll() {
    scrollElementToTop(window, 500);
    document.querySelectorAll('.info-concept, .content-panels-wrapper, .center-content, [id$="contentPanels"], [id$="cardsContainer"]').forEach(function(el) {
        if (el && el.scrollTop > 0) {
            scrollElementToTop(el, 700);
        }
    });
}
window.scrollToTopAll = scrollToTopAll;
window.scrollToTopOfPage = scrollToTopAll;

// Délégation : clic sur scrollTop -> scrollToTopAll
document.addEventListener('DOMContentLoaded', function() {
    document.body.addEventListener('click', function(e) {
        var btn = e.target && (e.target.closest('.ui-scrolltop') || e.target.closest('.scroll-top-button'));
        if (btn) {
            setTimeout(scrollToTopAll, 50);
        }
    });

    // Scroll en haut avec effet de glissement après confirmation de sauvegarde (param scrollTop dans l'URL)
    var urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('scrollTop') === '1' && typeof scrollToTopAll === 'function') {
        setTimeout(scrollToTopAll, 250);
        if (typeof history.replaceState === 'function') {
            urlParams.delete('scrollTop');
            var newSearch = urlParams.toString();
            var cleanUrl = window.location.pathname + (newSearch ? '?' + newSearch : '') + window.location.hash;
            history.replaceState({}, document.title, cleanUrl);
        }
    }
});
