/**
 * Compatibilité PrimeFaces 14 : les méthodes addOnStart, addOnComplete, addOnUpdate, addOnError
 * ont été supprimées. Ce script recrée cette API en wrappant PrimeFaces.ajax.Request.send.
 * À charger après constants.js et avant tout script utilisant PrimeFaces.ajax.addOn*.
 */
(function() {
    function initCompat() {
        if (typeof PrimeFaces === 'undefined' || !PrimeFaces.ajax || !PrimeFaces.ajax.Request) return;

        // Déjà compat (version antérieure à PF14)
        if (typeof PrimeFaces.ajax.addOnStart === 'function') return;

        var onStartCallbacks = [];
        var onCompleteCallbacks = [];
        var onUpdateCallbacks = [];
        var onErrorCallbacks = [];

        PrimeFaces.ajax.addOnStart = function(fn) { onStartCallbacks.push(fn); };
        PrimeFaces.ajax.addOnComplete = function(fn) { onCompleteCallbacks.push(fn); };
        PrimeFaces.ajax.addOnUpdate = function(fn) { onUpdateCallbacks.push(fn); };
        PrimeFaces.ajax.addOnError = function(fn) { onErrorCallbacks.push(fn); };

        var originalSend = PrimeFaces.ajax.Request.send;
        if (!originalSend) return;

        PrimeFaces.ajax.Request.send = function(cfg) {
            cfg = cfg || {};

            var origOnstart = cfg.onstart;
            cfg.onstart = function(req, config) {
                var settings = config || {};
                onStartCallbacks.forEach(function(fn) {
                    try { fn(null, settings, {}); } catch (e) { console.warn('addOnStart callback error', e); }
                });
                if (typeof origOnstart === 'function') origOnstart.apply(this, arguments);
            };

            var origOncomplete = cfg.oncomplete;
            cfg.oncomplete = function(xhrOrErr, status, pfArgs, dataOrXhr) {
                var args = pfArgs || {};
                onCompleteCallbacks.forEach(function(fn) {
                    try { fn(xhrOrErr, status, args); } catch (e) { console.warn('addOnComplete callback error', e); }
                });
                onUpdateCallbacks.forEach(function(fn) {
                    try { fn(dataOrXhr); } catch (e) { console.warn('addOnUpdate callback error', e); }
                });
                if (typeof origOncomplete === 'function') origOncomplete.apply(this, arguments);
            };

            var origOnerror = cfg.onerror;
            cfg.onerror = function(xhr, status, errorThrown) {
                onErrorCallbacks.forEach(function(fn) {
                    try { fn(xhr, status, errorThrown, {}); } catch (e) { console.warn('addOnError callback error', e); }
                });
                if (typeof origOnerror === 'function') origOnerror.apply(this, arguments);
            };

            return originalSend.apply(this, arguments);
        };
    }

    function tryInit() {
        initCompat();
        if (typeof PrimeFaces !== 'undefined' && PrimeFaces.ajax && typeof PrimeFaces.ajax.addOnStart === 'function') {
            return;
        }
        var elapsed = 0;
        var check = function() {
            elapsed += 50;
            initCompat();
            if (elapsed < 2000 && (typeof PrimeFaces === 'undefined' || !PrimeFaces.ajax || typeof PrimeFaces.ajax.addOnStart !== 'function')) {
                setTimeout(check, 50);
            }
        };
        setTimeout(check, 100);
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', tryInit);
    } else {
        tryInit();
    }
})();
