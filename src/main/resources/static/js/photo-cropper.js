/* ==========================================================================
   photo-cropper.js - Crop pop-up for the "Profile Photo" input
   ========================================================================== */
(function () {
    'use strict';

    var CSS = [
        '.pc-overlay{position:fixed;inset:0;z-index:9999;display:none;align-items:center;justify-content:center;padding:16px;background:rgba(17,24,39,.6)}',
        '.pc-overlay.pc-open{display:flex}',
        '.pc-box{background:#fff;border-radius:12px;padding:20px;width:100%;max-width:380px;box-shadow:0 20px 50px rgba(0,0,0,.3);font-family:Arial,Helvetica,sans-serif}',
        '.pc-box h3{margin:0 0 4px;font-size:18px;color:#111827}',
        '.pc-help{margin:0 0 14px;font-size:13px;color:#6b7280}',
        '.pc-stage{position:relative;margin:0 auto;background:#f3f4f6;overflow:hidden;border-radius:8px;touch-action:none}',
        '.pc-stage canvas{display:block;cursor:grab;touch-action:none}',
        '.pc-stage canvas.pc-drag{cursor:grabbing}',
        '.pc-ring{position:absolute;inset:0;border-radius:50%;box-shadow:0 0 0 999px rgba(0,0,0,.5);border:2px solid #fff;pointer-events:none}',
        '.pc-zoomrow{display:flex;align-items:center;gap:10px;margin:14px 0 4px;color:#6b7280;font-size:13px}',
        '.pc-box input[type=range]{flex:1;width:auto;padding:0;border:0;margin:0}',
        '.pc-actions{display:flex;gap:10px;margin-top:16px}',
        '.pc-btn{flex:1;min-height:44px;border:0;border-radius:8px;background:#059669;color:#fff;font-size:15px;font-weight:700;cursor:pointer}',
        '.pc-btn:hover{background:#047857}',
        '.pc-btn--ghost{background:#fff;color:#374151;border:1px solid #d1d5db}',
        '.pc-btn--ghost:hover{background:#f3f4f6}',
        '.pc-btn:disabled{opacity:.6;cursor:wait}',
        '.pc-error{margin-top:6px;font-size:13px;font-weight:600;color:#b91c1c}'
    ].join('\n');

    function ready(fn) {
        if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', fn);
        else fn();
    }

    ready(function () {
        var input = document.querySelector('input[name="photoFile"]');
        if (!input || !window.DataTransfer || !HTMLCanvasElement.prototype.toBlob) return;

        var maxKb = parseInt(input.getAttribute('data-max-kb'), 10) || 50;
        var MAX_FILE_BYTES = maxKb * 1024;      // biggest file the user may pick
        var maxBytes = MAX_FILE_BYTES - 2048;   // target size of the cropped result

        var style = document.createElement('style');
        style.textContent = CSS;
        document.head.appendChild(style);

        /* red error text under the photo field */
        var errBox = document.createElement('small');
        errBox.className = 'pc-error';
        errBox.setAttribute('role', 'alert');
        errBox.style.display = 'none';
        input.parentNode.appendChild(errBox);

        function showError(msg) { errBox.textContent = msg; errBox.style.display = 'block'; }
        function clearError() { errBox.textContent = ''; errBox.style.display = 'none'; }

        /* crop pop-up */
        var overlay = document.createElement('div');
        overlay.className = 'pc-overlay';
        overlay.innerHTML =
            '<div class="pc-box" role="dialog" aria-modal="true" aria-label="Crop profile photo">' +
              '<h3>Crop profile photo</h3>' +
              '<p class="pc-help">Drag the picture to move it. Use the slider to zoom.</p>' +
              '<div class="pc-stage"><canvas></canvas><div class="pc-ring"></div></div>' +
              '<div class="pc-zoomrow"><span>\u2212</span><input type="range" class="pc-zoom" min="1" max="4" step="0.01" value="1" aria-label="Zoom"><span>+</span></div>' +
              '<div class="pc-actions">' +
                '<button type="button" class="pc-btn pc-btn--ghost" data-act="cancel">Cancel</button>' +
                '<button type="button" class="pc-btn" data-act="ok">Crop &amp; use photo</button>' +
              '</div>' +
            '</div>';
        document.body.appendChild(overlay);

        var stage = overlay.querySelector('.pc-stage');
        var canvas = overlay.querySelector('canvas');
        var ctx = canvas.getContext('2d');
        var zoomEl = overlay.querySelector('.pc-zoom');
        var okBtn = overlay.querySelector('[data-act="ok"]');

        var S = 300, img = null, imgUrl = null;
        var base = 1, zoom = 1, ox = 0, oy = 0;
        var croppedFile = null;

        function scale() { return base * zoom; }

        function clamp() {
            var w = img.naturalWidth * scale(), h = img.naturalHeight * scale();
            ox = Math.min(0, Math.max(S - w, ox));
            oy = Math.min(0, Math.max(S - h, oy));
        }

        function draw() {
            ctx.fillStyle = '#fff';
            ctx.fillRect(0, 0, S, S);
            ctx.drawImage(img, ox, oy, img.naturalWidth * scale(), img.naturalHeight * scale());
        }

        function setZoom(z) {
            var old = scale();
            var cx = (S / 2 - ox) / old, cy = (S / 2 - oy) / old;
            zoom = Math.min(4, Math.max(1, z));
            zoomEl.value = zoom;
            ox = S / 2 - cx * scale();
            oy = S / 2 - cy * scale();
            clamp();
            draw();
        }

        function openCropper(file) {
            if (!file || !/^image\//.test(file.type)) {
                showError('Please choose an image file (JPG, PNG, GIF or WEBP).');
                restoreAfterCancel();
                return;
            }
            if (imgUrl) URL.revokeObjectURL(imgUrl);
            imgUrl = URL.createObjectURL(file);
            var im = new Image();
            im.onload = function () {
                img = im;
                S = Math.max(200, Math.min(300, window.innerWidth - 72));
                canvas.width = S; canvas.height = S;
                stage.style.width = S + 'px'; stage.style.height = S + 'px';
                base = S / Math.min(im.naturalWidth, im.naturalHeight);
                zoom = 1; zoomEl.value = 1;
                ox = (S - im.naturalWidth * base) / 2;
                oy = (S - im.naturalHeight * base) / 2;
                clamp(); draw();
                overlay.classList.add('pc-open');
                document.body.style.overflow = 'hidden';
                okBtn.disabled = false;
                okBtn.focus();
            };
            im.onerror = function () {
                showError('This picture could not be read. Please try a JPG or PNG.');
                restoreAfterCancel();
            };
            im.src = imgUrl;
        }

        function closeCropper() {
            overlay.classList.remove('pc-open');
            document.body.style.overflow = '';
        }

        function restoreAfterCancel() {
            closeCropper();
            if (croppedFile) setInputFile(croppedFile); else input.value = '';
        }

        function setInputFile(file) {
            var dt = new DataTransfer();
            dt.items.add(file);
            input.files = dt.files;
        }

        function toJpeg(size, quality) {
            return new Promise(function (resolve) {
                var c = document.createElement('canvas');
                c.width = size; c.height = size;
                var cx = c.getContext('2d');
                cx.fillStyle = '#fff';
                cx.fillRect(0, 0, size, size);
                var sc = scale();
                cx.imageSmoothingQuality = 'high';
                cx.drawImage(img, -ox / sc, -oy / sc, S / sc, S / sc, 0, 0, size, size);
                c.toBlob(resolve, 'image/jpeg', quality);
            });
        }

        async function makeSmallJpeg() {
            var sizes = [300, 256, 220, 190, 160, 128, 96];
            var qualities = [0.92, 0.85, 0.78, 0.7, 0.6, 0.5, 0.4];
            var blob = null;
            for (var i = 0; i < sizes.length; i++) {
                for (var j = 0; j < qualities.length; j++) {
                    blob = await toJpeg(sizes[i], qualities[j]);
                    if (blob && blob.size <= maxBytes) return blob;
                }
            }
            return blob;
        }

        /* events */
        input.addEventListener('change', function () {
            var f = input.files && input.files[0];
            if (!f) return;

            if (f.size > MAX_FILE_BYTES) {
                showError('Photo is too large (' + Math.ceil(f.size / 1024) + ' KB). '
                    + 'Please upload a photo of ' + maxKb + ' KB or less.');
                if (croppedFile) setInputFile(croppedFile); else input.value = '';
                return;
            }
            clearError();
            openCropper(f);
        });

        overlay.addEventListener('click', async function (e) {
            var act = e.target && e.target.getAttribute && e.target.getAttribute('data-act');
            if (act === 'cancel' || e.target === overlay) {
                restoreAfterCancel();
            } else if (act === 'ok') {
                okBtn.disabled = true;
                var blob = await makeSmallJpeg();
                if (!blob || blob.size > maxBytes + 2048) {
                    okBtn.disabled = false;
                    restoreAfterCancel();
                    showError('Could not make the photo small enough. Please choose a simpler picture.');
                    return;
                }
                croppedFile = new File([blob], 'profile.jpg', { type: 'image/jpeg' });
                setInputFile(croppedFile);
                closeCropper();
            }
        });

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && overlay.classList.contains('pc-open')) {
                restoreAfterCancel();
            }
        });

        zoomEl.addEventListener('input', function () { setZoom(parseFloat(zoomEl.value)); });

        canvas.addEventListener('wheel', function (e) {
            e.preventDefault();
            setZoom(zoom + (e.deltaY < 0 ? 0.1 : -0.1));
        }, { passive: false });

        var dragging = false, lx = 0, ly = 0;
        canvas.addEventListener('pointerdown', function (e) {
            dragging = true; lx = e.clientX; ly = e.clientY;
            canvas.setPointerCapture(e.pointerId);
            canvas.classList.add('pc-drag');
        });
        canvas.addEventListener('pointermove', function (e) {
            if (!dragging) return;
            ox += e.clientX - lx; oy += e.clientY - ly;
            lx = e.clientX; ly = e.clientY;
            clamp(); draw();
        });
        function endDrag() { dragging = false; canvas.classList.remove('pc-drag'); }
        canvas.addEventListener('pointerup', endDrag);
        canvas.addEventListener('pointercancel', endDrag);
    });
})();