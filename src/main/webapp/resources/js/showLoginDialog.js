// Définir showLoginDialog immédiatement pour éviter les erreurs de référence
// Cette fonction doit être disponible avant que le header ne soit rendu
(function() {
    'use strict';
    
    // Définir la fonction immédiatement pour éviter les erreurs de référence
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
