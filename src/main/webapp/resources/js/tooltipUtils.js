/**
 * OpenTypo — système d'infobulles unifié (délégation document + data-tooltip).
 * Remplace les tooltips natifs du navigateur et harmonise l'affichage sur toute l'app.
 */
(function (global) {
    'use strict';

    const CONFIG = global.AppConstants && global.AppConstants.TOOLTIP
        ? global.AppConstants.TOOLTIP
        : {
            SHOW_DELAY: 400,
            HIDE_DELAY: 80,
            GAP: 8,
            MAX_WIDTH: 320,
            VIEWPORT_PADDING: 8
        };

    const SKIP_SELECTOR = [
        '.app-sidebar',
        '.sidebar-menu',
        '[data-tooltip-skip]',
        '#opentypo-tooltip'
    ].join(', ');

    let tipEl = null;
    let activeTarget = null;
    let showTimer = null;
    let hideTimer = null;

    function ensureTipElement() {
        if (!tipEl) {
            tipEl = document.createElement('div');
            tipEl.id = 'opentypo-tooltip';
            tipEl.setAttribute('role', 'tooltip');
            tipEl.hidden = true;
            document.body.appendChild(tipEl);
        }
        return tipEl;
    }

    function shouldSkip(el) {
        if (!el || !el.closest) {
            return true;
        }
        return !!el.closest(SKIP_SELECTOR);
    }

    function findTooltipTarget(el) {
        if (!el || !el.closest) {
            return null;
        }
        return el.closest('[data-tooltip]');
    }

    function getTooltipText(el) {
        const text = el.getAttribute('data-tooltip');
        return text ? text.trim() : '';
    }

    function ensureAriaLabel(el, text) {
        if (!text) {
            return;
        }
        const tag = el.tagName ? el.tagName.toLowerCase() : '';
        const needsLabel = tag === 'a' || tag === 'button' || el.getAttribute('role') === 'button';
        if (needsLabel && !el.getAttribute('aria-label') && !el.getAttribute('aria-labelledby')) {
            el.setAttribute('aria-label', text);
        }
    }

    /**
     * Copie title → data-tooltip pour éviter la bulle native du navigateur.
     */
    function migrateTitles(root) {
        const scope = root && root.querySelectorAll ? root : document;
        scope.querySelectorAll('[title]').forEach(function (el) {
            if (shouldSkip(el)) {
                return;
            }
            const title = el.getAttribute('title');
            if (!title || !title.trim()) {
                return;
            }
            if (!el.hasAttribute('data-tooltip')) {
                el.setAttribute('data-tooltip', title.trim());
            }
            ensureAriaLabel(el, title.trim());
            el.removeAttribute('title');
        });
    }

    function clearTimers() {
        if (showTimer) {
            clearTimeout(showTimer);
            showTimer = null;
        }
        if (hideTimer) {
            clearTimeout(hideTimer);
            hideTimer = null;
        }
    }

    function hide() {
        clearTimers();
        activeTarget = null;
        if (!tipEl) {
            return;
        }
        tipEl.classList.remove('opentypo-tooltip--visible');
        tipEl.hidden = true;
        tipEl.removeAttribute('data-for');
    }

    function clamp(value, min, max) {
        return Math.min(Math.max(value, min), max);
    }

    function positionTooltip(target, positionPref) {
        const tip = ensureTipElement();
        const rect = target.getBoundingClientRect();
        const gap = CONFIG.GAP || 8;
        const pad = CONFIG.VIEWPORT_PADDING || 8;

        tip.style.setProperty('--opentypo-tooltip-arrow-offset', '50%');
        tip.classList.remove(
            'opentypo-tooltip--top',
            'opentypo-tooltip--bottom',
            'opentypo-tooltip--left',
            'opentypo-tooltip--right'
        );

        const positions = positionPref
            ? [positionPref, 'top', 'bottom', 'right', 'left']
            : ['top', 'bottom', 'right', 'left'];

        const tipRect = tip.getBoundingClientRect();
        let chosen = positions[0];
        let top = 0;
        let left = 0;

        for (let i = 0; i < positions.length; i += 1) {
            const pos = positions[i];
            let t = 0;
            let l = 0;

            if (pos === 'top') {
                t = rect.top - tipRect.height - gap;
                l = rect.left + rect.width / 2 - tipRect.width / 2;
            } else if (pos === 'bottom') {
                t = rect.bottom + gap;
                l = rect.left + rect.width / 2 - tipRect.width / 2;
            } else if (pos === 'left') {
                t = rect.top + rect.height / 2 - tipRect.height / 2;
                l = rect.left - tipRect.width - gap;
            } else if (pos === 'right') {
                t = rect.top + rect.height / 2 - tipRect.height / 2;
                l = rect.right + gap;
            }

            const fitsVert = t >= pad && t + tipRect.height <= window.innerHeight - pad;
            const fitsHoriz = l >= pad && l + tipRect.width <= window.innerWidth - pad;

            if (fitsVert && fitsHoriz) {
                chosen = pos;
                top = t;
                left = l;
                break;
            }
            if (i === positions.length - 1) {
                chosen = pos;
                top = t;
                left = l;
            }
        }

        top = clamp(top, pad, window.innerHeight - tipRect.height - pad);
        left = clamp(left, pad, window.innerWidth - tipRect.width - pad);

        const targetCenterX = rect.left + rect.width / 2;
        const targetCenterY = rect.top + rect.height / 2;
        let arrowOffset = '50%';

        if (chosen === 'top' || chosen === 'bottom') {
            const arrowX = targetCenterX - left;
            arrowOffset = clamp(arrowX, 12, tipRect.width - 12) + 'px';
        } else {
            const arrowY = targetCenterY - top;
            arrowOffset = clamp(arrowY, 12, tipRect.height - 12) + 'px';
        }

        tip.style.top = top + 'px';
        tip.style.left = left + 'px';
        tip.style.setProperty('--opentypo-tooltip-arrow-offset', arrowOffset);
        tip.classList.add('opentypo-tooltip--' + chosen);
    }

    function show(target) {
        const text = getTooltipText(target);
        if (!text) {
            return;
        }

        const tip = ensureTipElement();
        const positionPref = target.getAttribute('data-tooltip-position') || 'top';

        tip.textContent = text;
        tip.hidden = false;
        tip.setAttribute('data-for', target.id || '');
        tip.classList.remove('opentypo-tooltip--visible');

        positionTooltip(target, positionPref);

        requestAnimationFrame(function () {
            positionTooltip(target, positionPref);
            tip.classList.add('opentypo-tooltip--visible');
        });

        activeTarget = target;
    }

    function scheduleShow(target) {
        clearTimers();
        showTimer = setTimeout(function () {
            show(target);
        }, CONFIG.SHOW_DELAY || 400);
    }

    function scheduleHide() {
        clearTimers();
        hideTimer = setTimeout(hide, CONFIG.HIDE_DELAY || 80);
    }

    function onPointerOver(event) {
        const target = findTooltipTarget(event.target);
        if (!target || shouldSkip(target)) {
            return;
        }
        if (activeTarget === target) {
            return;
        }
        hide();
        scheduleShow(target);
    }

    function onPointerOut(event) {
        if (!activeTarget) {
            return;
        }
        const related = event.relatedTarget;
        if (related && (activeTarget.contains(related) || findTooltipTarget(related) === activeTarget)) {
            return;
        }
        scheduleHide();
    }

    function onFocusIn(event) {
        const target = findTooltipTarget(event.target);
        if (!target || shouldSkip(target)) {
            return;
        }
        hide();
        show(target);
    }

    function onFocusOut(event) {
        if (!activeTarget) {
            return;
        }
        const related = event.relatedTarget;
        if (related && activeTarget.contains(related)) {
            return;
        }
        scheduleHide();
    }

    function onScroll() {
        if (activeTarget) {
            hide();
        }
    }

    function bindPrimeFacesDefaults() {
        if (typeof PrimeFaces === 'undefined' || !PrimeFaces.widget || !PrimeFaces.widget.Tooltip) {
            return;
        }
        const defaults = {
            showDelay: CONFIG.SHOW_DELAY || 400,
            hideDelay: CONFIG.HIDE_DELAY || 120,
            showEffect: 'fade',
            hideEffect: 'fade',
            styleClass: 'opentypo-tooltip',
            position: 'top',
            escape: true
        };
        Object.assign(PrimeFaces.widget.Tooltip.prototype.cfg, defaults);
    }

    function init() {
        ensureTipElement();
        migrateTitles(document);
        bindPrimeFacesDefaults();

        document.addEventListener('mouseover', onPointerOver, true);
        document.addEventListener('mouseout', onPointerOut, true);
        document.addEventListener('focusin', onFocusIn, true);
        document.addEventListener('focusout', onFocusOut, true);
        window.addEventListener('scroll', onScroll, true);
        window.addEventListener('resize', onScroll);

        if (typeof PrimeFaces !== 'undefined') {
            $(document).on('pfAjaxComplete', function (_e, xhr, settings) {
                const update = settings && settings.update;
                if (!update) {
                    migrateTitles(document);
                    return;
                }
                const ids = update.split(/\s+/);
                ids.forEach(function (id) {
                    const el = document.getElementById(id);
                    if (el) {
                        migrateTitles(el);
                    }
                });
            });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    global.OpenTypoTooltip = {
        init: init,
        migrateTitles: migrateTitles,
        hide: hide
    };
})(window);
