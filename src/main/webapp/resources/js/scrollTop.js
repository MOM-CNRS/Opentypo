(function() {
    'use strict';
    var THRESHOLD = 150;
    var btn = document.getElementById('scrollTopBtn');
    if (!btn) return;
    function isScrolledDown() {
        if (window.scrollY > THRESHOLD) return true;
        var scrollables = document.querySelectorAll('.info-concept, [id$="contentPanels"], [id$="cardsContainer"]');
        for (var i = 0; i < scrollables.length; i++) {
            if (scrollables[i].scrollTop > THRESHOLD) return true;
        }
        return false;
    }
    function updateVisibility() {
        if (isScrolledDown()) {
            btn.classList.add('ui-scrolltop-visible');
        } else {
            btn.classList.remove('ui-scrolltop-visible');
        }
    }
    function scrollToTop() {
        window.scrollTo({ top: 0, behavior: 'smooth' });
        document.querySelectorAll('.info-concept, [id$="contentPanels"], [id$="cardsContainer"]').forEach(function(el) {
            if (el && el.scrollTop > 0) el.scrollTo({ top: 0, behavior: 'smooth' });
        });
    }
    function bindScrollListeners() {
        var scrollables = document.querySelectorAll('.info-concept, [id$="contentPanels"], [id$="cardsContainer"]');
        scrollables.forEach(function(el) {
            if (!el.dataset.scrollTopBound) {
                el.dataset.scrollTopBound = '1';
                el.addEventListener('scroll', updateVisibility, { passive: true });
            }
        });
    }
    btn.addEventListener('click', scrollToTop);
    window.addEventListener('scroll', updateVisibility, { passive: true });
    bindScrollListeners();
    updateVisibility();
    if (typeof MutationObserver !== 'undefined') {
        new MutationObserver(function() {
            bindScrollListeners();
            updateVisibility();
        }).observe(document.body, { childList: true, subtree: true });
    }
})();
