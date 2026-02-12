/**
 * Page Erreur - affichage des détails d'erreur depuis l'URL
 */
(function () {
    'use strict';

    function showErrorDetails() {
        var detailsDiv = document.getElementById('errorDetails');
        var detailsText = document.getElementById('errorDetailsText');
        if (!detailsDiv || !detailsText) return;

        var urlParams = new URLSearchParams(window.location.search);
        var errorMessage = urlParams.get('message') || urlParams.get('error') || 'Aucun détail disponible';
        try {
            detailsText.textContent = decodeURIComponent(errorMessage);
        } catch (e) {
            detailsText.textContent = errorMessage;
        }
        detailsDiv.style.display = detailsDiv.style.display === 'none' ? 'block' : 'none';
    }

    function init() {
        var detailsDiv = document.getElementById('errorDetails');
        var detailsText = document.getElementById('errorDetailsText');
        var urlParams = new URLSearchParams(window.location.search);
        var errorMessage = urlParams.get('message') || urlParams.get('error');
        if (errorMessage && detailsDiv && detailsText) {
            try {
                detailsText.textContent = decodeURIComponent(errorMessage);
            } catch (e) {
                detailsText.textContent = errorMessage;
            }
            detailsDiv.style.display = 'block';
            detailsDiv.classList.remove('error-details-hidden');
        }
        var btn = document.getElementById('showErrorDetailsBtn');
        if (btn) {
            btn.addEventListener('click', showErrorDetails);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
