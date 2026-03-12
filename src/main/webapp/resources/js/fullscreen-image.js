/**
 * Script pour gérer l'affichage d'images en plein écran.
 * Exposé sur window pour être appelé par actions.js (data-action="show-fullscreen-image").
 * @param {string} imageUrl - URL de l'image
 * @param {string} [legend] - Légende optionnelle à afficher sous l'image
 */
(function() {
    'use strict';

function showFullscreenImage(imageUrl, legend) {
    const overlay = document.getElementById('fullscreenImageOverlay');
    const imageElement = document.getElementById('fullscreenImageElement');
    const legendElement = document.getElementById('fullscreenImageLegend');
    
    if (overlay) {
        if (imageElement) {
            imageElement.src = imageUrl;
            overlay.classList.add('active');
            document.body.style.overflow = 'hidden';
        }
        if (legendElement) {
            legendElement.textContent = legend && legend.trim ? legend.trim() : '';
            legendElement.style.display = (legend && legend.trim && legend.trim().length > 0) ? 'block' : 'none';
        }
    }
}

function hideFullscreenImage() {
    const overlay = document.getElementById('fullscreenImageOverlay');
    const legendElement = document.getElementById('fullscreenImageLegend');
    
    if (overlay) {
        overlay.classList.remove('active');
        document.body.style.overflow = '';
    }
    if (legendElement) {
        legendElement.textContent = '';
        legendElement.style.display = 'none';
    }
}

    window.showFullscreenImage = showFullscreenImage;
    window.hideFullscreenImage = hideFullscreenImage;

    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' || event.keyCode === 27) {
            hideFullscreenImage();
            var galleriaWrapper = document.querySelector('.galleria-main-wrapper.galleria-fullscreen-mode');
            if (galleriaWrapper) {
                galleriaWrapper.classList.remove('galleria-fullscreen-mode');
                document.body.style.overflow = '';
            }
        }
    });
})();
