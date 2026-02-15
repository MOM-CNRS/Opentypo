/**
 * Galerie d'images custom - OpenTypo
 * Navigation, vignettes, clic plein écran, Échap pour fermer
 */
(function () {
    'use strict';

    window.OpentypoGallery = window.OpentypoGallery || {};

    function initGalleries() {
        document.querySelectorAll('.opentypo-gallery').forEach(function (gallery) {
            if (gallery.dataset.initialized === 'true') return;
            gallery.dataset.initialized = 'true';

            var slides = gallery.querySelectorAll('.opentypo-gallery-slide');
            var thumbnails = gallery.querySelectorAll('.opentypo-gallery-thumb');
            var captionItems = gallery.querySelectorAll('.opentypo-gallery-caption-item');
            var prevBtn = gallery.querySelector('.opentypo-gallery-prev');
            var nextBtn = gallery.querySelector('.opentypo-gallery-next');
            var currentIndex = 0;
            var total = slides.length;

            function showSlide(index) {
                if (total === 0) return;
                currentIndex = (index + total) % total;

                slides.forEach(function (s, i) {
                    s.classList.toggle('opentypo-gallery-slide-active', i === currentIndex);
                    s.setAttribute('aria-hidden', i !== currentIndex);
                });
                thumbnails.forEach(function (t, i) {
                    t.classList.toggle('opentypo-gallery-thumb-active', i === currentIndex);
                    t.setAttribute('aria-selected', i === currentIndex);
                });
                captionItems.forEach(function (c, i) {
                    c.style.display = i === currentIndex ? 'block' : 'none';
                });

                if (prevBtn) prevBtn.style.visibility = total <= 1 ? 'hidden' : 'visible';
                if (nextBtn) nextBtn.style.visibility = total <= 1 ? 'hidden' : 'visible';
            }

            prevBtn && prevBtn.addEventListener('click', function () { showSlide(currentIndex - 1); });
            nextBtn && nextBtn.addEventListener('click', function () { showSlide(currentIndex + 1); });

            thumbnails.forEach(function (thumb, i) {
                thumb.addEventListener('click', function () { showSlide(i); });
            });

            showSlide(0);
        });
    }

    window.OpentypoGallery.init = initGalleries;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initGalleries);
    } else {
        initGalleries();
    }
})();
