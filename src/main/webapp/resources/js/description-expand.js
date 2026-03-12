/**
 * Gestion du bouton "afficher la suite" / "afficher moins" pour les descriptions longues.
 * Logique basée sur le nombre de caractères (400) : si > 400, affichage tronqué + bouton.
 */
(function() {
    'use strict';

    var scheduleTimeouts = [];

    function handleToggleClick(ev) {
        var toggle = ev.target.closest('[data-description-toggle="true"]');
        if (!toggle) return;
        ev.preventDefault();
        ev.stopPropagation();
        var wrapper = toggle.closest('.concept-info-description-wrapper');
        if (!wrapper) return;

        var truncated = wrapper.querySelector('[data-description-truncated="true"]');
        var full = wrapper.querySelector('[data-description-full="true"]');
        if (truncated && full) {
            var isExpanded = wrapper.classList.contains('concept-info-description-expanded');
            if (isExpanded) {
                wrapper.classList.remove('concept-info-description-expanded');
                toggle.textContent = 'afficher la suite';
                toggle.setAttribute('aria-expanded', 'false');
            } else {
                wrapper.classList.add('concept-info-description-expanded');
                toggle.textContent = 'Afficher moins';
                toggle.setAttribute('aria-expanded', 'true');
            }
        }
    }

    function initDescriptionExpand() {
        document.querySelectorAll('.concept-info-description-char-based').forEach(function(wrapper) {
            wrapper.classList.remove('concept-info-description-expanded');
        });
    }

    function scheduleInit() {
        scheduleTimeouts.forEach(function(t) { clearTimeout(t); });
        scheduleTimeouts = [];
        initDescriptionExpand();
        scheduleTimeouts.push(setTimeout(initDescriptionExpand, 150));
        scheduleTimeouts.push(setTimeout(initDescriptionExpand, 400));
        scheduleTimeouts.push(setTimeout(initDescriptionExpand, 700));
    }

    function runWhenReady() {
        scheduleInit();
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', scheduleInit);
        }
        window.addEventListener('load', function() {
            scheduleInit();
            setTimeout(scheduleInit, 200);
            setTimeout(scheduleInit, 600);
        });
    }

    runWhenReady();

    document.addEventListener('click', handleToggleClick, true);

    if (typeof PrimeFaces !== 'undefined' && PrimeFaces.ajax) {
        PrimeFaces.ajax.addOnUpdate(function() {
            scheduleInit();
        });
        PrimeFaces.ajax.addOnComplete(function() {
            scheduleInit();
        });
    }

    if (typeof MutationObserver !== 'undefined') {
        function setupObserver() {
            var target = document.getElementById('centerContent') || document.querySelector('[id$="centerContent"]') || document.getElementById('contentPanelsWrapper') || document.querySelector('.content-panels-wrapper');
            if (target && !target._descriptionObserverSet) {
                target._descriptionObserverSet = true;
                var observer = new MutationObserver(function(mutations) {
                    var hasRelevantChange = mutations.some(function(m) {
                        return m.addedNodes && m.addedNodes.length > 0;
                    });
                    if (hasRelevantChange) scheduleInit();
                });
                observer.observe(target, { childList: true, subtree: true });
            }
        }
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', setupObserver);
        } else {
            setupObserver();
        }
    }

    window.initDescriptionExpand = initDescriptionExpand;
})();
