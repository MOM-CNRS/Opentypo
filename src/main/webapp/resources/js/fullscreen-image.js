/**
 * Script pour gérer l'affichage d'images en plein écran.
 * Exposé sur window pour être appelé par actions.js (data-action="show-fullscreen-image").
 */
(function() {
    'use strict';

function showFullscreenImage(imageUrl) {
    const overlay = document.getElementById('fullscreenImageOverlay');
    const imageElement = document.getElementById('fullscreenImageElement');
    
    if (overlay) {
        if (imageElement) {
            imageElement.src = imageUrl;
            overlay.classList.add('active');
            document.body.style.overflow = 'hidden';
        }
    }
}

function hideFullscreenImage() {
    const overlay = document.getElementById('fullscreenImageOverlay');
    
    if (overlay) {
        overlay.classList.remove('active');
        document.body.style.overflow = '';
    }
}

    window.showFullscreenImage = showFullscreenImage;
    window.hideFullscreenImage = hideFullscreenImage;

    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' || event.keyCode === 27) {
            hideFullscreenImage();
        }
    });
})();
