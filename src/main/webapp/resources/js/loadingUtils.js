/**
 * Utilitaires pour gérer les animations de chargement
 */

// Variable globale pour suivre l'état du chargement
let isLoading = false;
let loadingTimeout = null;
let activeAjaxRequests = 0; // Compteur de requêtes AJAX actives
let safetyTimeout = null; // Timeout de sécurité pour masquer le loader

/**
 * Affiche l'overlay de chargement global
 * @param {string} message - Message optionnel à afficher
 * @param {string} type - Type de loader ('spinner', 'dots', 'bars', 'gradient')
 */
function showLoading(message, type) {
    type = type || 'spinner';
    message = message || 'Chargement...';
    
    // Incrémenter le compteur de requêtes
    activeAjaxRequests++;
    
    // Éviter les appels multiples si déjà actif
    if (isLoading) {
        return;
    }
    
    isLoading = true;
    
    // Annuler le timeout de sécurité s'il existe
    if (safetyTimeout) {
        clearTimeout(safetyTimeout);
        safetyTimeout = null;
    }
    
    // Créer l'overlay s'il n'existe pas
    let overlay = document.getElementById('globalLoadingOverlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'globalLoadingOverlay';
        overlay.className = 'loading-overlay';
        overlay.setAttribute('data-created-time', Date.now().toString());
        document.body.appendChild(overlay);
    }
    
    // Créer le conteneur
    let container = overlay.querySelector('.loading-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'loading-container';
        overlay.appendChild(container);
    }
    
    // Vider le conteneur
    container.innerHTML = '';
    
    // Ajouter le loader selon le type
    let loaderElement = null;
    switch(type) {
        case 'dots':
            loaderElement = createDotsLoader();
            break;
        case 'bars':
            loaderElement = createBarsLoader();
            break;
        case 'gradient':
            loaderElement = createGradientLoader();
            break;
        default:
            loaderElement = createSpinnerLoader();
    }
    
    container.appendChild(loaderElement);
    
    // Ajouter le texte
    const textElement = document.createElement('p');
    textElement.className = 'loading-text';
    textElement.textContent = message;
    container.appendChild(textElement);
    
    // Enregistrer l'heure de début
    overlay.setAttribute('data-start-time', Date.now().toString());
    
    // Afficher avec un léger délai pour éviter les clignotements
    setTimeout(() => {
        overlay.classList.add('active');
    }, 10);
    
    // Timeout de sécurité : masquer le loader après 10 secondes maximum
    safetyTimeout = setTimeout(function() {
        console.warn('Loader timeout - masquage automatique après 10 secondes');
        forceHideLoading();
    }, 10000);
}

/**
 * Masque l'overlay de chargement global
 */
function hideLoading() {
    // Décrémenter le compteur de requêtes
    if (activeAjaxRequests > 0) {
        activeAjaxRequests--;
    }
    
    // Ne masquer que si toutes les requêtes sont terminées
    if (activeAjaxRequests > 0) {
        return;
    }
    
    forceHideLoading();
}

/**
 * Force le masquage du loader (utilisé en cas de timeout ou d'erreur)
 */
function forceHideLoading() {
    if (!isLoading) {
        return;
    }
    
    isLoading = false;
    activeAjaxRequests = 0;
    
    // Annuler le timeout de sécurité
    if (safetyTimeout) {
        clearTimeout(safetyTimeout);
        safetyTimeout = null;
    }
    
    const overlay = document.getElementById('globalLoadingOverlay');
    if (overlay) {
        overlay.classList.remove('active');
        overlay.removeAttribute('data-start-time');
        overlay.removeAttribute('data-created-time');
        // Retirer l'overlay du DOM après l'animation
        setTimeout(() => {
            if (overlay && !overlay.classList.contains('active')) {
                overlay.remove();
            }
        }, 300);
    }
    
    // Annuler le timeout s'il existe
    if (loadingTimeout) {
        clearTimeout(loadingTimeout);
        loadingTimeout = null;
    }
}

/**
 * Crée un spinner circulaire
 */
function createSpinnerLoader() {
    const spinner = document.createElement('div');
    spinner.className = 'loading-spinner';
    return spinner;
}

/**
 * Crée un loader avec points animés
 */
function createDotsLoader() {
    const container = document.createElement('div');
    container.className = 'loading-dots';
    for (let i = 0; i < 3; i++) {
        const dot = document.createElement('div');
        dot.className = 'loading-dot';
        container.appendChild(dot);
    }
    return container;
}

/**
 * Crée un loader avec barres animées
 */
function createBarsLoader() {
    const container = document.createElement('div');
    container.className = 'loading-bars';
    for (let i = 0; i < 5; i++) {
        const bar = document.createElement('div');
        bar.className = 'loading-bar';
        container.appendChild(bar);
    }
    return container;
}

/**
 * Crée un loader avec gradient
 */
function createGradientLoader() {
    const spinner = document.createElement('div');
    spinner.className = 'loading-spinner-gradient';
    return spinner;
}

/**
 * Affiche un loader avec timeout automatique
 * @param {number} timeout - Délai en millisecondes avant masquage automatique
 */
function showLoadingWithTimeout(timeout, message, type) {
    showLoading(message, type);
    loadingTimeout = setTimeout(() => {
        hideLoading();
    }, timeout);
}

/**
 * Affiche un loader pour un bouton spécifique
 * @param {HTMLElement|string} button - Élément bouton ou son ID
 */
function showButtonLoading(button) {
    if (typeof button === 'string') {
        button = document.getElementById(button);
    }
    
    if (!button) {
        return;
    }
    
    // Vérifier si le loader existe déjà
    if (button.querySelector('.button-loader')) {
        return;
    }
    
    // Désactiver le bouton
    button.disabled = true;
    
    // Créer le loader
    const loader = document.createElement('span');
    loader.className = 'button-loader';
    button.insertBefore(loader, button.firstChild);
}

/**
 * Masque le loader d'un bouton
 * @param {HTMLElement|string} button - Élément bouton ou son ID
 */
function hideButtonLoading(button) {
    if (typeof button === 'string') {
        button = document.getElementById(button);
    }
    
    if (!button) {
        return;
    }
    
    // Réactiver le bouton
    button.disabled = false;
    
    // Retirer le loader
    const loader = button.querySelector('.button-loader');
    if (loader) {
        loader.remove();
    }
}

/**
 * Affiche un loader inline dans un élément
 * @param {HTMLElement|string} element - Élément ou son ID
 */
function showInlineLoading(element) {
    if (typeof element === 'string') {
        element = document.getElementById(element);
    }
    
    if (!element) {
        return;
    }
    
    // Vérifier si le loader existe déjà
    if (element.querySelector('.inline-loader')) {
        return;
    }
    
    const loader = document.createElement('span');
    loader.className = 'inline-loader';
    element.appendChild(loader);
}

/**
 * Masque le loader inline d'un élément
 * @param {HTMLElement|string} element - Élément ou son ID
 */
function hideInlineLoading(element) {
    if (typeof element === 'string') {
        element = document.getElementById(element);
    }
    
    if (!element) {
        return;
    }
    
    const loader = element.querySelector('.inline-loader');
    if (loader) {
        loader.remove();
    }
}

/**
 * Initialise les listeners pour PrimeFaces AJAX
 */
function initPrimeFacesLoading() {
    // Méthode 1 : Utiliser les callbacks natifs de PrimeFaces (plus fiable)
    if (typeof PrimeFaces !== 'undefined') {
        // Avant le début d'une requête AJAX
        PrimeFaces.ajax.addOnStart(function(xhr, settings, args) {
            // Ne pas afficher le loader global si un loader spécifique est déjà géré
            if (settings && settings.source) {
                const sourceElement = document.querySelector(settings.source);
                if (sourceElement) {
                    // Si le bouton a déjà un loader spécifique, ne pas afficher le loader global
                    if (sourceElement.classList.contains('category-btn-validate') || 
                        sourceElement.classList.contains('collection-create-btn') ||
                        sourceElement.closest('.reference-card')) {
                        return;
                    }
                }
            }
            // Afficher le loader global avec un léger délai pour éviter les clignotements
            setTimeout(function() {
                showLoading('Chargement...', 'spinner');
            }, 100);
        });
        
        // Après la fin d'une requête AJAX (succès)
        PrimeFaces.ajax.addOnComplete(function(xhr, status, args) {
            // Masquer le loader après un court délai pour s'assurer que le DOM est mis à jour
            setTimeout(function() {
                hideLoading();
            }, 500);
        });
        
        // Callback après mise à jour du DOM
        PrimeFaces.ajax.addOnUpdate(function(data) {
            // Masquer le loader après la mise à jour du DOM
            setTimeout(function() {
                hideLoading();
            }, 600);
        });
        
        // En cas d'erreur
        PrimeFaces.ajax.addOnError(function(xhr, status, error, args) {
            forceHideLoading();
        });
    }
    
    // Méthode 2 : Écouter les événements jQuery (backup)
    if (typeof $ !== 'undefined') {
        // Avant le début d'une requête AJAX
        $(document).on('pfAjaxSend', function(event, xhr, settings) {
            // Ne pas afficher le loader global si un loader spécifique est déjà géré
            if (settings && settings.source) {
                const source = $(settings.source);
                if (source.hasClass('category-btn-validate') || 
                    source.hasClass('collection-create-btn') ||
                    source.closest('.reference-card').length > 0) {
                    return;
                }
            }
            // Afficher le loader global avec un léger délai
            setTimeout(function() {
                showLoading('Chargement...', 'spinner');
            }, 100);
        });
        
        // Après la fin d'une requête AJAX
        $(document).on('pfAjaxComplete', function(event, xhr, settings) {
            setTimeout(function() {
                hideLoading();
            }, 500);
        });
        
        // En cas d'erreur
        $(document).on('pfAjaxError', function(event, xhr, settings, errorThrown) {
            forceHideLoading();
        });
        
        // Événement de succès AJAX
        $(document).on('pfAjaxSuccess', function(event, xhr, settings) {
            setTimeout(function() {
                hideLoading();
            }, 500);
        });
    }
    
    // Méthode 3 : Surveiller les mutations du DOM pour détecter la fin des mises à jour
    if (typeof MutationObserver !== 'undefined') {
        let updateTimeout = null;
        let lastMutationTime = 0;
        const observer = new MutationObserver(function(mutations) {
            // Si le loader est actif et qu'il y a des mutations
            if (isLoading) {
                lastMutationTime = Date.now();
                // Annuler le timeout précédent
                if (updateTimeout) {
                    clearTimeout(updateTimeout);
                }
                // Masquer le loader après 800ms sans nouvelles mutations
                updateTimeout = setTimeout(function() {
                    const timeSinceLastMutation = Date.now() - lastMutationTime;
                    // Vérifier que toutes les requêtes sont terminées et qu'il n'y a pas eu de mutations récentes
                    if (activeAjaxRequests === 0 && timeSinceLastMutation >= 800) {
                        forceHideLoading();
                    }
                }, 800);
            }
        });
        
        // Observer les changements dans le body et dans centerContent spécifiquement
        observer.observe(document.body, {
            childList: true,
            subtree: true,
            attributes: false
        });
        
        // Observer aussi spécifiquement le conteneur principal
        const centerContent = document.getElementById('centerContent');
        if (centerContent) {
            observer.observe(centerContent, {
                childList: true,
                subtree: true,
                attributes: false
            });
        }
    }
    
    // Méthode 4 : Surveiller l'état de la requête XMLHttpRequest (optionnel, peut causer des conflits)
    // Désactivé par défaut pour éviter les conflits avec PrimeFaces
    /*
    if (typeof XMLHttpRequest !== 'undefined') {
        const originalOpen = XMLHttpRequest.prototype.open;
        const originalSend = XMLHttpRequest.prototype.send;
        
        XMLHttpRequest.prototype.open = function(method, url, async, user, password) {
            this._url = url;
            return originalOpen.apply(this, arguments);
        };
        
        XMLHttpRequest.prototype.send = function(data) {
            const xhr = this;
            
            // Vérifier si c'est une requête PrimeFaces
            if (this._url && (this._url.indexOf('javax.faces.resource') === -1 && 
                             this._url.indexOf('primefaces') !== -1 || 
                             this._url.indexOf('/faces/') !== -1)) {
                
                xhr.addEventListener('loadend', function() {
                    setTimeout(function() {
                        hideLoading();
                    }, 100);
                });
                
                xhr.addEventListener('error', function() {
                    forceHideLoading();
                });
            }
            
            return originalSend.apply(this, arguments);
        };
    }
    */
}

// Initialiser au chargement du DOM
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initPrimeFacesLoading);
} else {
    initPrimeFacesLoading();
}

// Masquer le loader si la page est rechargée ou si l'utilisateur navigue
window.addEventListener('beforeunload', function() {
    forceHideLoading();
});

// Masquer le loader au chargement de la page (sécurité)
window.addEventListener('load', function() {
    setTimeout(function() {
        forceHideLoading();
    }, 500);
});

// Détecter quand PrimeFaces a fini de mettre à jour le DOM
// En surveillant les changements dans les composants PrimeFaces
if (typeof PrimeFaces !== 'undefined') {
    // Attendre que PrimeFaces soit complètement initialisé
    setTimeout(function() {
        // Surveiller les mises à jour des composants
        const originalUpdate = PrimeFaces.ajax && PrimeFaces.ajax.Response && PrimeFaces.ajax.Response.handle;
        if (originalUpdate) {
            PrimeFaces.ajax.Response.handle = function(responseXML, responseText, status, xhr) {
                const result = originalUpdate.call(this, responseXML, responseText, status, xhr);
                // Masquer le loader après le traitement de la réponse
                setTimeout(function() {
                    hideLoading();
                }, 600);
                return result;
            };
        }
    }, 1000);
}

// Vérification périodique pour s'assurer que le loader n'est pas bloqué
setInterval(function() {
    // Si le loader est actif depuis plus de 5 secondes, le forcer à se masquer
    const overlay = document.getElementById('globalLoadingOverlay');
    if (overlay && overlay.classList.contains('active')) {
        const activeTime = overlay.getAttribute('data-start-time');
        if (activeTime) {
            const elapsed = Date.now() - parseInt(activeTime);
            if (elapsed > 5000) {
                console.warn('Loader actif depuis plus de 5 secondes - masquage forcé');
                forceHideLoading();
            }
        } else {
            // Si pas de timestamp, masquer après 3 secondes par sécurité
            const createdTime = overlay.getAttribute('data-created-time') || Date.now();
            const elapsed = Date.now() - parseInt(createdTime);
            if (elapsed > 3000) {
                console.warn('Loader actif sans timestamp valide - masquage forcé');
                forceHideLoading();
            }
        }
    }
}, 500);

// Variables pour le loader de panel
let panelLoadingActive = false;
let panelLoadingOverlay = null;

/**
 * Affiche le spinner dans un panel spécifique (rightTreePanel)
 * @param {string} panelId - ID du panel où afficher le spinner
 * @param {string} message - Message optionnel à afficher
 * @param {string} type - Type de loader ('spinner', 'dots', 'bars', 'gradient')
 */
function showPanelLoading(panelId, message, type) {
    type = type || 'spinner';
    message = message || 'Chargement...';
    
    if (panelLoadingActive) {
        return;
    }
    
    panelLoadingActive = true;
    
    const panel = document.getElementById(panelId);
    if (!panel) {
        console.warn('Panel non trouvé:', panelId);
        return;
    }
    
    // Créer l'overlay s'il n'existe pas
    panelLoadingOverlay = document.getElementById('panelLoadingOverlay');
    if (!panelLoadingOverlay) {
        panelLoadingOverlay = document.createElement('div');
        panelLoadingOverlay.id = 'panelLoadingOverlay';
        panelLoadingOverlay.className = 'panel-loading-overlay';
        panel.appendChild(panelLoadingOverlay);
    }
    
    // Créer le conteneur
    let container = panelLoadingOverlay.querySelector('.panel-loading-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'panel-loading-container';
        panelLoadingOverlay.appendChild(container);
    }
    
    // Vider le conteneur
    container.innerHTML = '';
    
    // Ajouter le loader selon le type
    let loaderElement = null;
    switch(type) {
        case 'dots':
            loaderElement = createDotsLoader();
            break;
        case 'bars':
            loaderElement = createBarsLoader();
            break;
        case 'gradient':
            loaderElement = createGradientLoader();
            break;
        default:
            loaderElement = createSpinnerLoader();
    }
    
    container.appendChild(loaderElement);
    
    // Ajouter le texte
    const textElement = document.createElement('p');
    textElement.className = 'loading-text';
    textElement.textContent = message;
    container.appendChild(textElement);
    
    // Afficher avec un léger délai
    setTimeout(() => {
        panelLoadingOverlay.classList.add('active');
    }, 10);
    
    // Bloquer l'arbre pendant le chargement
    blockTree();
}

/**
 * Masque le spinner du panel
 */
function hidePanelLoading() {
    if (!panelLoadingActive) {
        return;
    }
    
    panelLoadingActive = false;
    
    if (panelLoadingOverlay) {
        panelLoadingOverlay.classList.remove('active');
        // Retirer l'overlay du DOM après l'animation
        setTimeout(() => {
            if (panelLoadingOverlay && !panelLoadingOverlay.classList.contains('active')) {
                panelLoadingOverlay.remove();
                panelLoadingOverlay = null;
            }
        }, 300);
    }
    
    // Débloquer l'arbre
    unblockTree();
}

/**
 * Bloque l'interaction avec l'arbre
 */
function blockTree() {
    const treeContainer = document.getElementById('treeContainer');
    if (treeContainer) {
        treeContainer.classList.add('tree-blocked');
    }
}

/**
 * Débloque l'interaction avec l'arbre
 */
function unblockTree() {
    const treeContainer = document.getElementById('treeContainer');
    if (treeContainer) {
        treeContainer.classList.remove('tree-blocked');
    }
}
