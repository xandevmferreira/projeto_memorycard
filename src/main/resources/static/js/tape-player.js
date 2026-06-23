(function () {
    const root = document.getElementById('tape-player-root');
    if (!root) {
        return;
    }

    const gameId = root.dataset.gameId;
    const cartridgeId = root.dataset.cartridgeId;
    const core = root.dataset.core;
    const stateUrl = root.dataset.stateUrl || '';
    const saveUrl = root.dataset.saveUrl || '';
    const hasState = root.dataset.hasState === 'true';
    const romInput = document.getElementById('tape-rom-input');
    const romStatus = document.getElementById('tape-rom-status');
    const messageEl = document.getElementById('tape-player-message');
    const saveBtn = document.getElementById('tape-save-btn');
    const emulatorHost = document.getElementById('tape-emulator');

    const DB_NAME = 'memorycard-tape-player';
    const STORE = 'roms';
    const EJS_CDN = 'https://cdn.emulatorjs.org/stable/data/';
    let romObjectUrl = null;
    let emulatorStarted = false;

    function showMessage(text) {
        if (!messageEl) {
            return;
        }
        messageEl.textContent = text;
        messageEl.classList.remove('hidden');
    }

    function openRomDb() {
        return new Promise(function (resolve, reject) {
            const req = indexedDB.open(DB_NAME, 1);
            req.onupgradeneeded = function () {
                req.result.createObjectStore(STORE);
            };
            req.onsuccess = function () { resolve(req.result); };
            req.onerror = function () { reject(req.error); };
        });
    }

    function loadRomFromDb() {
        return openRomDb().then(function (db) {
            return new Promise(function (resolve, reject) {
                const tx = db.transaction(STORE, 'readonly');
                const get = tx.objectStore(STORE).get('game-' + gameId);
                get.onsuccess = function () { resolve(get.result || null); };
                get.onerror = function () { reject(get.error); };
            });
        });
    }

    function saveRomToDb(record) {
        return openRomDb().then(function (db) {
            return new Promise(function (resolve, reject) {
                const tx = db.transaction(STORE, 'readwrite');
                tx.objectStore(STORE).put(record, 'game-' + gameId);
                tx.oncomplete = function () { resolve(); };
                tx.onerror = function () { reject(tx.error); };
            });
        });
    }

    function loadScript(src) {
        return new Promise(function (resolve, reject) {
            if (document.querySelector('script[data-ejs-loader]')) {
                resolve();
                return;
            }
            const script = document.createElement('script');
            script.src = src;
            script.async = true;
            script.dataset.ejsLoader = 'true';
            script.onload = resolve;
            script.onerror = reject;
            document.body.appendChild(script);
        });
    }

    function startEmulator(romBlob, romName) {
        if (emulatorStarted) {
            if (emulatorHost) {
                emulatorHost.innerHTML = '';
            }
            emulatorStarted = false;
        }
        var placeholder = document.getElementById('tape-emulator-placeholder');
        if (placeholder) {
            placeholder.remove();
        }
        if (romObjectUrl) {
            URL.revokeObjectURL(romObjectUrl);
        }
        romObjectUrl = URL.createObjectURL(romBlob);

        window.EJS_player = '#tape-emulator';
        window.EJS_core = core;
        window.EJS_pathtodata = EJS_CDN;
        window.EJS_gameUrl = romObjectUrl;
        window.EJS_gameName = romName || 'jogo';
        window.EJS_startOnLoaded = true;
        window.EJS_color = '#1a1a2e';
        window.EJS_backgroundColor = '#0f0f1a';

        if (hasState && stateUrl) {
            window.EJS_loadStateURL = stateUrl;
        } else {
            window.EJS_loadStateURL = '';
        }

        window.EJS_onGameStart = function () {
            emulatorStarted = true;
            if (saveBtn) {
                saveBtn.disabled = false;
            }
            if (romStatus) {
                romStatus.textContent = 'Rodando: ' + romName + (hasState ? ' — save state carregado.' : '');
                romStatus.className = 'alert alert-success tape-rom-status';
            }
        };

        return loadScript(EJS_CDN + 'loader.js').catch(function (err) {
            showMessage('Não foi possível carregar o emulador. Verifique sua internet ou bloqueador de anúncios.');
            throw err;
        });
    }

    function handleRomFile(file) {
        if (!file) {
            return;
        }
        const record = { name: file.name, blob: file, savedAt: Date.now() };
        saveRomToDb(record).then(function () {
            if (romStatus) {
                romStatus.textContent = 'ROM carregada: ' + file.name + ' — iniciando console...';
            }
            return startEmulator(file, file.name);
        }).catch(function (err) {
            showMessage('Não foi possível iniciar o emulador: ' + (err && err.message ? err.message : err));
        });
    }

    if (romInput) {
        romInput.addEventListener('change', function () {
            handleRomFile(romInput.files && romInput.files[0]);
        });
    }

    if (saveBtn) {
        saveBtn.addEventListener('click', function () {
            if (!window.EJS_emulator || !window.EJS_emulator.gameManager) {
                showMessage('Aguarde o jogo carregar antes de salvar.');
                return;
            }
            try {
                const state = window.EJS_emulator.gameManager.getState();
                if (!state) {
                    showMessage('Não foi possível capturar o estado do emulador.');
                    return;
                }
                const blob = new Blob([state], { type: 'application/octet-stream' });
                const form = new FormData();
                form.append('file', blob, 'browser-save.state');
                form.append('fileType', 'STATE');
                saveBtn.disabled = true;
                fetch('/api/games/' + gameId + '/cartridges/' + cartridgeId + '/quick-save', {
                    method: 'POST',
                    body: form
                }).then(function (res) {
                    if (!res.ok) {
                        throw new Error('HTTP ' + res.status);
                    }
                    if (romStatus) {
                        romStatus.textContent = 'Progresso salvo nesta fita!';
                        romStatus.className = 'alert alert-success tape-rom-status';
                    }
                }).catch(function () {
                    showMessage('Falha ao salvar progresso no site.');
                }).finally(function () {
                    saveBtn.disabled = false;
                });
            } catch (e) {
                showMessage('Erro ao salvar: ' + e.message);
            }
        });
    }

    loadRomFromDb().then(function (record) {
        if (record && record.blob) {
            if (romStatus) {
                romStatus.textContent = 'ROM lembrada: ' + record.name + ' — iniciando...';
            }
            return startEmulator(record.blob, record.name);
        }
        if (romStatus) {
            romStatus.textContent = 'Escolha a ROM (' + (root.dataset.romHint || '') + ') para inserir a fita.';
        }
    }).catch(function () {
        /* IndexedDB indisponível — usuário escolhe ROM manualmente */
    });
})();
