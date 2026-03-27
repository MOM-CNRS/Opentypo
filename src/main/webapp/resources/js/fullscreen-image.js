/**
 * Affichage plein écran avec navigation (flèches latérales, clavier ←/→, compteur).
 * @see showFullscreenImage(url, legend)
 * @see showFullscreenImage(url, legend, { items: [{url, legend}], index })
 * @see openFullscreenFromGallery(imgElement) pour la galerie type
 */
(function() {
    'use strict';

    /** @type {{ mode: 'single'|'gallery', singleUrl: string, singleLegend: string, items: Array<{url:string,legend:string}>, index: number }} */
    var state = {
        mode: 'single',
        singleUrl: '',
        singleLegend: '',
        items: [],
        index: 0
    };

    function getOverlay() {
        return document.getElementById('fullscreenImageOverlay');
    }

    function isOverlayActive() {
        var o = getOverlay();
        return o && o.classList.contains('active');
    }

    function renderFullscreenContent() {
        var imageElement = document.getElementById('fullscreenImageElement');
        var legendElement = document.getElementById('fullscreenImageLegend');
        var prevBtn = document.getElementById('fullscreenImagePrev');
        var nextBtn = document.getElementById('fullscreenImageNext');
        var counterEl = document.getElementById('fullscreenImageCounter');

        var url = '';
        var legend = '';
        if (state.mode === 'gallery' && state.items.length > 0) {
            var item = state.items[state.index];
            url = item.url;
            legend = item.legend || '';
        } else {
            url = state.singleUrl;
            legend = state.singleLegend || '';
        }

        if (imageElement) {
            imageElement.src = url;
            imageElement.alt = legend ? legend : 'Image en plein écran';
        }
        if (legendElement) {
            var leg = legend != null ? String(legend).trim() : '';
            legendElement.textContent = leg;
            legendElement.style.display = leg.length > 0 ? 'block' : 'none';
        }

        var showNav = state.mode === 'gallery' && state.items.length > 1;
        if (prevBtn) {
            prevBtn.style.display = showNav ? 'flex' : 'none';
            prevBtn.disabled = !showNav || state.index <= 0;
            prevBtn.setAttribute('aria-disabled', (!showNav || state.index <= 0) ? 'true' : 'false');
        }
        if (nextBtn) {
            nextBtn.style.display = showNav ? 'flex' : 'none';
            nextBtn.disabled = !showNav || state.index >= state.items.length - 1;
            nextBtn.setAttribute('aria-disabled', (!showNav || state.index >= state.items.length - 1) ? 'true' : 'false');
        }
        if (counterEl) {
            if (showNav) {
                counterEl.style.display = 'flex';
                renderFullscreenDots(counterEl);
            } else {
                counterEl.style.display = 'none';
                counterEl.innerHTML = '';
                counterEl.removeAttribute('aria-label');
            }
        }
    }

    /**
     * Points : un par image ; le point actif est mis en évidence (couleur charte).
     * Clic sur un point pour aller à cette image.
     */
    function renderFullscreenDots(container) {
        var total = state.items.length;
        var current = state.index;
        container.innerHTML = '';
        container.setAttribute('aria-label', 'Image ' + (current + 1) + ' sur ' + total);
        for (var i = 0; i < total; i++) {
            var dot = document.createElement('button');
            dot.type = 'button';
            dot.className = 'fullscreen-image-dot' + (i === current ? ' fullscreen-image-dot-active' : '');
            dot.setAttribute('aria-label', 'Image ' + (i + 1) + ' sur ' + total);
            dot.setAttribute('aria-current', i === current ? 'true' : 'false');
            dot.title = 'Image ' + (i + 1) + ' / ' + total;
            (function(index) {
                dot.addEventListener('click', function(e) {
                    e.stopPropagation();
                    if (!isOverlayActive() || state.mode !== 'gallery') return;
                    state.index = index;
                    renderFullscreenContent();
                });
            })(i);
            container.appendChild(dot);
        }
    }

    /**
     * @param {string} imageUrl
     * @param {string} [legend]
     * @param {{ items: Array<{url:string, legend?:string}>, index: number }} [galleryOptions]
     */
    function showFullscreenImage(imageUrl, legend, galleryOptions) {
        var overlay = getOverlay();
        var imageElement = document.getElementById('fullscreenImageElement');

        if (galleryOptions && galleryOptions.items && galleryOptions.items.length > 1) {
            state.mode = 'gallery';
            state.items = galleryOptions.items.map(function(it) {
                return {
                    url: it.url || '',
                    legend: it.legend != null ? String(it.legend).trim() : ''
                };
            });
            state.index = Math.max(0, Math.min(galleryOptions.index != null ? galleryOptions.index : 0, state.items.length - 1));
        } else {
            state.mode = 'single';
            state.singleUrl = imageUrl || '';
            state.singleLegend = legend != null ? String(legend).trim() : '';
            state.items = [];
            state.index = 0;
        }

        if (overlay && imageElement) {
            renderFullscreenContent();
            overlay.classList.add('active');
            document.body.style.overflow = 'hidden';
        }
    }

    function hideFullscreenImage() {
        var overlay = getOverlay();
        var legendElement = document.getElementById('fullscreenImageLegend');

        if (overlay) {
            overlay.classList.remove('active');
            document.body.style.overflow = '';
        }
        if (legendElement) {
            legendElement.textContent = '';
            legendElement.style.display = 'none';
        }
        var prevBtn = document.getElementById('fullscreenImagePrev');
        var nextBtn = document.getElementById('fullscreenImageNext');
        var counterEl = document.getElementById('fullscreenImageCounter');
        if (prevBtn) prevBtn.style.display = 'none';
        if (nextBtn) nextBtn.style.display = 'none';
        if (counterEl) {
            counterEl.style.display = 'none';
            counterEl.innerHTML = '';
            counterEl.removeAttribute('aria-label');
        }
        state.mode = 'single';
        state.items = [];
        state.index = 0;

        var galleriaWrapper = document.querySelector('.galleria-main-wrapper.galleria-fullscreen-mode');
        if (galleriaWrapper) {
            galleriaWrapper.classList.remove('galleria-fullscreen-mode');
            document.body.style.overflow = '';
        }
    }

    function fullscreenImageGoPrev() {
        if (!isOverlayActive() || state.mode !== 'gallery' || state.items.length <= 1) return;
        if (state.index <= 0) return;
        state.index -= 1;
        renderFullscreenContent();
    }

    function fullscreenImageGoNext() {
        if (!isOverlayActive() || state.mode !== 'gallery' || state.items.length <= 1) return;
        if (state.index >= state.items.length - 1) return;
        state.index += 1;
        renderFullscreenContent();
    }

    /**
     * Clic sur une image dans .opentypo-gallery : ouvre le plein écran avec toutes les images de la galerie.
     * @param {HTMLImageElement} img
     */
    function openFullscreenFromGallery(img) {
        if (!img || !img.src) return;
        var gallery = img.closest('.opentypo-gallery');
        if (!gallery) {
            showFullscreenImage(img.src, (img.getAttribute('data-image-legende') || ''));
            return;
        }
        var imgs = gallery.querySelectorAll('.opentypo-gallery-slide img');
        var items = [];
        var idx = 0;
        imgs.forEach(function(im, i) {
            if (!im || !im.src) return;
            items.push({
                url: im.src,
                legend: (im.getAttribute('data-image-legende') || '').trim()
            });
            if (im === img) idx = i;
        });
        if (items.length === 0) return;
        if (items.length === 1) {
            showFullscreenImage(items[0].url, items[0].legend);
            return;
        }
        showFullscreenImage(items[idx].url, items[idx].legend, { items: items, index: idx });
    }

    window.showFullscreenImage = showFullscreenImage;
    window.hideFullscreenImage = hideFullscreenImage;
    window.openFullscreenFromGallery = openFullscreenFromGallery;
    window.fullscreenImageGoPrev = fullscreenImageGoPrev;
    window.fullscreenImageGoNext = fullscreenImageGoNext;

    document.addEventListener('keydown', function(event) {
        if (!isOverlayActive()) return;

        if (event.key === 'Escape' || event.keyCode === 27) {
            hideFullscreenImage();
            return;
        }

        if (state.mode === 'gallery' && state.items.length > 1) {
            if (event.key === 'ArrowLeft' || event.keyCode === 37) {
                event.preventDefault();
                fullscreenImageGoPrev();
                return;
            }
            if (event.key === 'ArrowRight' || event.keyCode === 39) {
                event.preventDefault();
                fullscreenImageGoNext();
                return;
            }
        }
    });
})();
