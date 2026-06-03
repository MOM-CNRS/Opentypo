/**
 * Navigation rapide des fiches détail (type, etc.) : scroll dans le panneau .info-concept
 * et résolution des IDs JSF (suffixe après le dernier ':').
 */
(function () {
    'use strict';

    function findElementByAnchorId(id) {
        if (!id) {
            return null;
        }
        var exact = document.getElementById(id);
        if (exact) {
            return exact;
        }
        try {
            return document.querySelector('[id$="' + CSS.escape(id) + '"]');
        } catch (e) {
            return document.querySelector('[id$="' + id.replace(/"/g, '\\"') + '"]');
        }
    }

    function findScrollContainer(el) {
        var node = el ? el.parentElement : null;
        while (node && node !== document.body) {
            var style = window.getComputedStyle(node);
            var scrollable = style.overflowY === 'auto' || style.overflowY === 'scroll'
                || style.overflow === 'auto' || style.overflow === 'scroll';
            if (scrollable && node.scrollHeight > node.clientHeight + 1) {
                return node;
            }
            node = node.parentElement;
        }
        return null;
    }

    function scrollToAnchorTarget(id) {
        var target = findElementByAnchorId(id);
        if (!target) {
            return false;
        }

        var nav = target.closest('.type-detail-page--v2')
            ? target.closest('.type-detail-page--v2').querySelector('.type-page-nav')
            : document.querySelector('.type-page-nav');
        var offset = nav ? nav.getBoundingClientRect().height + 12 : 52;
        var container = findScrollContainer(target);

        if (container) {
            var containerRect = container.getBoundingClientRect();
            var targetRect = target.getBoundingClientRect();
            var nextTop = container.scrollTop + (targetRect.top - containerRect.top) - offset;
            container.scrollTo({ top: Math.max(0, nextTop), behavior: 'smooth' });
        } else {
            var pageTop = target.getBoundingClientRect().top + window.pageYOffset - offset;
            window.scrollTo({ top: Math.max(0, pageTop), behavior: 'smooth' });
        }

        if (window.history && window.history.replaceState) {
            window.history.replaceState(null, '', '#' + id);
        }
        return true;
    }

    function handleNavClick(event) {
        var link = event.target.closest('.type-page-nav a.type-page-nav__pill[href^="#"]');
        if (!link) {
            return;
        }
        var hash = link.getAttribute('href');
        if (!hash || hash.length < 2) {
            return;
        }
        var id = hash.substring(1);
        if (!findElementByAnchorId(id)) {
            return;
        }
        event.preventDefault();
        scrollToAnchorTarget(id);
    }

    function scrollToInitialHash() {
        if (!window.location.hash || window.location.hash.length < 2) {
            return;
        }
        var id = decodeURIComponent(window.location.hash.substring(1));
        if (findElementByAnchorId(id)) {
            window.setTimeout(function () {
                scrollToAnchorTarget(id);
            }, 100);
        }
    }

    document.addEventListener('click', handleNavClick);

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', scrollToInitialHash);
    } else {
        scrollToInitialHash();
    }

    if (window.jQuery) {
        jQuery(document).on('pfAjaxComplete', function () {
            scrollToInitialHash();
        });
    }
})();
