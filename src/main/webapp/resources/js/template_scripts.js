// Fonction pour mettre à jour l'item actif du menu
function updateActiveMenuItem() {
    const currentPath = window.location.pathname;
    const menuItems = document.querySelectorAll('.sidebar-menu-item');
    let hasActiveItem = false;

    // Retirer la classe active de tous les items
    menuItems.forEach(function(menuItem) {
        menuItem.classList.remove('active');
    });

    menuItems.forEach(function(item) {
        const link = item.querySelector('a') || item;
        const href = link.getAttribute('href') || link.getAttribute('action') || '';
        const menuItemType = item.getAttribute('data-menu-item') || '';
        const itemText = item.querySelector('.sidebar-menu-item-text')?.textContent?.trim() || '';

        let shouldBeActive = false;

        // Utiliser l'attribut data-menu-item pour une détection plus précise
        if (menuItemType === 'accueil') {
            if (currentPath === '/' ||
                currentPath === '/index.xhtml' ||
                currentPath.endsWith('/index.xhtml') ||
                currentPath === '' ||
                (currentPath === '/index.xhtml' && !currentPath.includes('/users/') && !currentPath.includes('/candidats/'))) {
                shouldBeActive = true;
            }
        }
        else if (menuItemType === 'candidats') {
            // Actif pour toutes les pages sous /candidats/
            if (currentPath.includes('/candidats/') ||
                currentPath.includes('/candidats') ||
                href.includes('/candidats/')) {
                shouldBeActive = true;
            }
        }
        else if (menuItemType === 'users') {
            // Actif pour toutes les pages sous /users/
            if (currentPath.includes('/users/') ||
                currentPath.includes('/users') ||
                href.includes('/users/')) {
                shouldBeActive = true;
            }
        }
        // Fallback : détection basée sur le texte si data-menu-item n'est pas disponible
        else {
            if (itemText === 'Accueil' || itemText.includes('Accueil')) {
                if (currentPath === '/' || currentPath === '/index.xhtml' || currentPath.endsWith('/index.xhtml')) {
                    shouldBeActive = true;
                }
            }
            else if (itemText === 'Candidats' || itemText.includes('Candidats')) {
                if (currentPath.includes('/candidats/') || currentPath.includes('/candidats')) {
                    shouldBeActive = true;
                }
            }
            else if (itemText.includes('utilisateurs') || itemText.includes('utilisateur')) {
                if (currentPath.includes('/users/') || currentPath.includes('/users')) {
                    shouldBeActive = true;
                }
            }
        }

        if (shouldBeActive) {
            item.classList.add('active');
            hasActiveItem = true;
        }
    });

    // Si aucun item n'est actif et qu'on est sur la page d'accueil, sélectionner Accueil
    if (!hasActiveItem && (currentPath === '/' || currentPath === '/index.xhtml' || currentPath.endsWith('/index.xhtml'))) {
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

// Gestion globale de l'expiration de session
(function() {
    let sessionCheckInterval = null;
    let sessionExpiredShown = false;
    let isChecking = false;

    // Fonction pour rediriger vers index.xhtml
    function redirectToIndex(reason) {
        if (sessionExpiredShown) {
            return; // Éviter les redirections multiples
        }

        sessionExpiredShown = true;

        // Arrêter le polling
        if (sessionCheckInterval) {
            clearInterval(sessionCheckInterval);
            sessionCheckInterval = null;
        }

        // Afficher le message d'expiration via growl
        if (typeof PrimeFaces !== 'undefined') {
            PrimeFaces.showMessage({
                summary: 'Déconnexion automatique',
                detail: 'Votre session a expiré après 1 heure d\'inactivité. Vous avez été déconnecté automatiquement et redirigé vers la page d\'accueil. Veuillez vous reconnecter pour continuer.',
                severity: 'warn',
                life: 8000 // Afficher le message pendant 8 secondes
            });
        }

        // Rediriger vers index.xhtml immédiatement (utiliser replace pour éviter de revenir en arrière)
        const redirectUrl = window.location.pathname.includes('/index.xhtml')
            ? window.location.pathname + '?sessionExpired=true'
            : '/index.xhtml?sessionExpired=true';

        // Utiliser replace pour forcer la navigation
        window.location.replace(redirectUrl);
    }

    // Fonction pour vérifier l'état de la session via polling
    function checkSessionStatus() {
        // Éviter les vérifications multiples simultanées
        if (isChecking) {
            return;
        }

        // Ne pas vérifier si on est déjà en train de rediriger
        if (sessionExpiredShown) {
            return;
        }

        isChecking = true;

        fetch('/session-check', {
            method: 'GET',
            credentials: 'include',
            cache: 'no-cache',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(function(response) {
            if (response.ok) {
                return response.json();
            }
            // Si la réponse n'est pas OK, considérer que la session est expirée
            throw new Error('Session check failed');
        })
        .then(function(data) {
            isChecking = false;

            // Si la session n'est plus valide ou l'utilisateur n'est plus authentifié
            if (!data || !data.valid || !data.authenticated) {
                redirectToIndex('sessionExpired');
            }
        })
        .catch(function(error) {
            isChecking = false;

            // En cas d'erreur (y compris 403, 401, etc.), considérer que la session est expirée
            // Ne rediriger que si on n'est pas déjà sur index.xhtml
            const currentPath = window.location.pathname;
            if (!currentPath.includes('/index.xhtml')) {
                if (currentPath !== '/') {
                    if (!currentPath.endsWith('/')) {
                        redirectToIndex('error');
                    }
                }
            }
        });
    }

    // Démarrer le polling dès que le DOM est prêt
    function startSessionMonitoring() {
        // Vérifier immédiatement au chargement
        checkSessionStatus();

        // Vérifier l'état de la session toutes les 3 secondes pour une détection rapide
        sessionCheckInterval = setInterval(checkSessionStatus, 3000);
    }

    // Démarrer le monitoring dès que possible
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', startSessionMonitoring);
    } else {
        // Le DOM est déjà chargé
        startSessionMonitoring();
    }

    // Vérifier aussi lors des événements utilisateur (pour détection immédiate)
    let userActivityTimeout = null;
    ['mousedown', 'keypress', 'click', 'touchstart', 'scroll'].forEach(function(event) {
        document.addEventListener(event, function() {
            // Annuler le timeout précédent
            if (userActivityTimeout) {
                clearTimeout(userActivityTimeout);
            }

            // Vérifier la session après une courte pause pour éviter trop de requêtes
            userActivityTimeout = setTimeout(function() {
                if (!sessionExpiredShown) {
                    checkSessionStatus();
                }
            }, 2000);
        }, true);
    });
})();

    // Vérifier si la session a expiré ou si la vue a expiré (paramètre dans l'URL)
    (function() {
        const urlParams = new URLSearchParams(window.location.search);
        const sessionExpired = urlParams.get('sessionExpired') === 'true';
        const viewExpired = urlParams.get('viewExpired') === 'true';

        if (sessionExpired || viewExpired) {
            // Fonction pour afficher le message de déconnexion via growl
            function showDisconnectionMessage() {
                // Vérifier que PrimeFaces et le growl sont disponibles
                if (typeof PrimeFaces !== 'undefined') {
                    const message = viewExpired
                        ? 'Votre session a expiré. Vous avez été déconnecté et redirigé vers la page d\'accueil. Veuillez vous reconnecter pour continuer.'
                        : 'Votre session a expiré après 1 heure d\'inactivité. Vous avez été déconnecté automatiquement. Veuillez vous reconnecter pour continuer.';

                    // Fonction pour afficher le message dans le growl
                    function showMessageInGrowl() {
                        // Chercher le growl de différentes manières
                        let growlElement = document.getElementById('growl');

                        if (!growlElement) {
                            // Chercher par classe
                            const growls = document.querySelectorAll('.ui-growl');
                            if (growls.length > 0) {
                                growlElement = growls[0];
                            }
                        }

                        if (!growlElement) {
                            return false;
                        }

                        // Trouver le conteneur .ui-growl (peut être l'élément lui-même ou un enfant)
                        let container = growlElement;
                        if (!growlElement.classList.contains('ui-growl')) {
                            container = growlElement.querySelector('.ui-growl');
                            if (!container) {
                                container = growlElement;
                            }
                        }

                        // Créer le message
                        const msgContainer = document.createElement('div');
                        msgContainer.className = 'ui-growl-item-container ui-growl-warn';

                        const msgItem = document.createElement('div');
                        msgItem.className = 'ui-growl-item ui-growl-message-warn';

                        const icon = document.createElement('span');
                        icon.className = 'ui-growl-icon ui-growl-icon-warn pi pi-exclamation-triangle';

                        const msgContent = document.createElement('div');
                        msgContent.className = 'ui-growl-message';

                        const title = document.createElement('span');
                        title.className = 'ui-growl-title';
                        title.textContent = 'Déconnexion automatique';

                        const detail = document.createElement('p');
                        detail.textContent = message;

                        const closeBtn = document.createElement('span');
                        closeBtn.className = 'ui-growl-close pi pi-times';
                        closeBtn.style.cursor = 'pointer';
                        closeBtn.onclick = function() {
                            msgContainer.style.opacity = '0';
                            setTimeout(function() {
                                if (msgContainer.parentNode) {
                                    msgContainer.remove();
                                }
                            }, 300);
                        };

                        msgContent.appendChild(title);
                        msgContent.appendChild(detail);
                        msgItem.appendChild(icon);
                        msgItem.appendChild(msgContent);
                        msgItem.appendChild(closeBtn);
                        msgContainer.appendChild(msgItem);

                        container.appendChild(msgContainer);

                        // Animation
                        setTimeout(function() {
                            msgContainer.style.opacity = '1';
                        }, 10);

                        // Auto-suppression après 10 secondes
                        setTimeout(function() {
                            if (msgContainer.parentNode) {
                                msgContainer.style.opacity = '0';
                                setTimeout(function() {
                                    if (msgContainer.parentNode) {
                                        msgContainer.remove();
                                    }
                                }, 300);
                            }
                        }, 10000);

                        return true;
                    }

                    // Essayer d'afficher le message
                    let msgAttempts = 0;
                    const maxMsgAttempts = 50;

                    function tryShowMessage() {
                        msgAttempts++;

                        if (showMessageInGrowl()) {
                            return;
                        }

                        if (msgAttempts < maxMsgAttempts) {
                            setTimeout(tryShowMessage, 200);
                        } else {
                            // Fallback : afficher une notification personnalisée
                            const notification = document.createElement('div');
                            notification.style.cssText = 'position:fixed;top:1.5rem;right:1.5rem;z-index:10000;' +
                                'background:#fff;border-left:4px solid #ffc107;border-radius:8px;' +
                                'box-shadow:0 2px 8px rgba(0,0,0,0.1);padding:1rem 1.25rem;' +
                                'max-width:380px;font-family:Roboto,sans-serif;';

                            // Créer la structure HTML du message
                            const notifContent = document.createElement('div');
                            notifContent.style.cssText = 'display:flex;align-items:flex-start;gap:0.875rem;';

                            const notifIcon = document.createElement('span');
                            notifIcon.className = 'pi pi-exclamation-triangle';
                            notifIcon.style.cssText = 'color:#ffc107;font-size:1.125rem;margin-top:0.125rem;';

                            const notifText = document.createElement('div');
                            notifText.style.cssText = 'flex:1;';

                            const notifTitle = document.createElement('div');
                            notifTitle.style.cssText = 'font-weight:600;font-size:0.9375rem;color:#333;margin-bottom:0.25rem;';
                            notifTitle.textContent = 'Déconnexion automatique';

                            const notifDetail = document.createElement('div');
                            notifDetail.style.cssText = 'font-size:0.875rem;color:#666;line-height:1.5;';
                            notifDetail.textContent = message;

                            const notifClose = document.createElement('span');
                            notifClose.className = 'pi pi-times';
                            notifClose.style.cssText = 'position:absolute;top:0.5rem;right:0.5rem;cursor:pointer;color:#819A91;font-size:0.875rem;';
                            notifClose.onclick = function() {
                                notification.style.opacity = '0';
                                setTimeout(function() {
                                    if (notification.parentNode) {
                                        notification.remove();
                                    }
                                }, 300);
                            };

                            notifText.appendChild(notifTitle);
                            notifText.appendChild(notifDetail);
                            notifContent.appendChild(notifIcon);
                            notifContent.appendChild(notifText);
                            notification.appendChild(notifContent);
                            notification.appendChild(notifClose);

                            document.body.appendChild(notification);

                            setTimeout(function() {
                                if (notification.parentNode) {
                                    notification.style.opacity = '0';
                                    notification.style.transition = 'opacity 0.3s ease';
                                    setTimeout(function() {
                                        if (notification.parentNode) {
                                            notification.remove();
                                        }
                                    }, 300);
                                }
                            }, 10000);
                        }
                    }

                    // Démarrer après un délai
                    setTimeout(tryShowMessage, 1000);

                    // Nettoyer l'URL après un court délai
                    setTimeout(function() {
                        window.history.replaceState({}, document.title, window.location.pathname);
                    }, 100);
                } else {
                    // Si PrimeFaces n'est pas encore chargé, réessayer
                    setTimeout(showDisconnectionMessage, 200);
                }
            }

            // Afficher le message dès que possible
            function initDisconnectionMessage() {
                // Attendre que le DOM et PrimeFaces soient complètement chargés
                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', function() {
                        setTimeout(showDisconnectionMessage, 500);
                    });
                } else {
                    // Le DOM est déjà chargé, attendre un peu pour PrimeFaces
                    setTimeout(showDisconnectionMessage, 500);
                }
            }

            // Initialiser l'affichage du message
            initDisconnectionMessage();

            // Appeler showOnlyCardsContainer si elle existe (avec délai pour s'assurer qu'elle est chargée)
            setTimeout(function() {
                if (typeof window.showOnlyCardsContainer === 'function') {
                    window.showOnlyCardsContainer();
                } else {
                    // Si la fonction n'existe pas encore, réessayer après un délai
                    setTimeout(function() {
                        if (typeof window.showOnlyCardsContainer === 'function') {
                            window.showOnlyCardsContainer();
                        }
                    }, 500);
                }
            }, 200);
        }
    })();

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
// Fonction pour afficher le dialog de login
// avec vérification que le widget est disponible
window.showLoginDialog = function() {
    var attempts = 0;
    var maxAttempts = 30;
    
    var checkDialog = function() {
        var dialog = PF('loginDialog');
        if (dialog) {
            dialog.show();
        } else if (attempts < maxAttempts) {
            attempts++;
            setTimeout(checkDialog, 100);
        } else {
            console.warn('Dialog loginDialog not found. Make sure the login dialog is included in the template.');
        }
    };
    
    checkDialog();
};

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
