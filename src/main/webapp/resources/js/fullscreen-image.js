/**
 * Script pour gérer l'affichage d'images en plein écran
 */

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

// Fermer avec la touche Échap
document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape' || event.keyCode === 27) {
        hideFullscreenImage();
    }
});
