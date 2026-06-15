document.addEventListener('DOMContentLoaded', () => {
    const titleInput = document.getElementById('title');
    const platformInput = document.getElementById('platform');
    const externalRatingInput = document.getElementById('externalRating');
    const externalCoverUrlInput = document.getElementById('externalCoverUrl');
    const suggestionsBox = document.getElementById('game-suggestions');
    const coverPreview = document.getElementById('cover-preview');
    const coverPreviewImg = document.getElementById('cover-preview-img');
    const searchHint = document.getElementById('search-hint');

    if (!titleInput || !suggestionsBox) {
        return;
    }

    let debounceTimer = null;
    let activeController = null;

    titleInput.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        const query = titleInput.value.trim();

        if (query.length < 2) {
            hideSuggestions();
            return;
        }

        debounceTimer = setTimeout(() => searchGames(query), 350);
    });

    document.addEventListener('click', (event) => {
        if (!suggestionsBox.contains(event.target) && event.target !== titleInput) {
            hideSuggestions();
        }
    });

    function searchGames(query) {
        if (activeController) {
            activeController.abort();
        }
        activeController = new AbortController();

        fetch(`/games/search-external?query=${encodeURIComponent(query)}`, {
            signal: activeController.signal,
            credentials: 'same-origin'
        })
            .then(response => {
                if (response.redirected || response.status === 401) {
                    throw new Error('Sessão expirada');
                }
                return response.ok ? response.json() : [];
            })
            .then(results => renderSuggestions(results))
            .catch(error => {
                if (error.name !== 'AbortError') {
                    hideSuggestions();
                    if (searchHint && error.message === 'Sessão expirada') {
                        searchHint.textContent = 'Sessão expirada. Faça login novamente.';
                    }
                }
            });
    }

    function renderSuggestions(results) {
        suggestionsBox.innerHTML = '';

        if (!results.length) {
            suggestionsBox.innerHTML = '<div class="suggestion-empty">Nenhum jogo encontrado. Continue digitando ou cadastre manualmente.</div>';
            suggestionsBox.classList.add('visible');
            return;
        }

        results.forEach(game => {
            const coverUrl = game.coverUrl;
            const item = document.createElement('button');
            item.type = 'button';
            item.className = 'suggestion-item';

            const cover = coverUrl
                ? `<img src="${escapeHtml(toProxyUrl(coverUrl))}" alt="" class="suggestion-cover" referrerpolicy="no-referrer">`
                : '<div class="suggestion-cover placeholder">🎮</div>';

            const platform = game.platform ? escapeHtml(game.platform) : 'Plataforma não informada';
            const rating = game.externalRating != null ? `★ ${game.externalRating}` : '';

            item.innerHTML = `
                ${cover}
                <div class="suggestion-info">
                    <strong>${escapeHtml(game.title)}</strong>
                    <span class="meta">${platform}${rating ? ' · ' + rating : ''}</span>
                </div>
            `;

            item.addEventListener('click', () => selectGame(game));
            suggestionsBox.appendChild(item);
        });

        suggestionsBox.classList.add('visible');
    }

    function selectGame(game) {
        titleInput.value = game.title || '';
        if (platformInput && game.platform) {
            platformInput.value = game.platform;
        }
        if (externalRatingInput && game.externalRating != null) {
            externalRatingInput.value = game.externalRating;
        }
        if (externalCoverUrlInput && game.coverUrl) {
            externalCoverUrlInput.value = game.coverUrl;
            showCoverPreview(game.coverUrl);
        }
        hideSuggestions();
        if (searchHint) {
            searchHint.textContent = 'Jogo selecionado! Capa e dados preenchidos automaticamente.';
        }
    }

    function showCoverPreview(url) {
        if (!coverPreview || !coverPreviewImg) {
            return;
        }
        coverPreviewImg.src = toProxyUrl(url);
        coverPreviewImg.referrerPolicy = 'no-referrer';
        coverPreview.classList.add('visible');
    }

    function toProxyUrl(url) {
        if (!url) {
            return '';
        }
        if (url.startsWith('/')) {
            return url;
        }
        if (url.includes('steamstatic.com')) {
            return url;
        }
        if (url.includes('media.rawg.io') || url.includes('images.rawg.io')) {
            return `/covers/proxy?url=${encodeURIComponent(url)}`;
        }
        return url;
    }

    function hideSuggestions() {
        suggestionsBox.classList.remove('visible');
        suggestionsBox.innerHTML = '';
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    const existingCover = coverPreviewImg?.dataset?.existing;
    if (existingCover) {
        coverPreviewImg.src = existingCover;
        coverPreviewImg.referrerPolicy = 'no-referrer';
        coverPreview.classList.add('visible');
    }
});
