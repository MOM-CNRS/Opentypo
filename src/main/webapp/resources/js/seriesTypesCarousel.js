/**
 * Carousel horizontal pour les types dans le composant Séries & Types unifié.
 */
(function () {
    'use strict';

    var SCROLL_STEP = 220;

    function findScrollContainer(button) {
        var wrapper = button && button.closest ? button.closest('.series-types-carousel-wrapper') : null;
        return wrapper ? wrapper.querySelector('.series-types-carousel-scroll') : null;
    }

    window.seriesTypesScrollPrev = function (button) {
        var scroll = findScrollContainer(button);
        if (scroll) {
            scroll.scrollBy({ left: -SCROLL_STEP, behavior: 'smooth' });
        }
    };

    window.seriesTypesScrollNext = function (button) {
        var scroll = findScrollContainer(button);
        if (scroll) {
            scroll.scrollBy({ left: SCROLL_STEP, behavior: 'smooth' });
        }
    };
})();
