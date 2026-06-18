document.addEventListener('DOMContentLoaded', () => {
    const retroCheckbox = document.getElementById('retro');
    const retroSection = document.getElementById('retro-section');
    const retroSearch = document.getElementById('retro-search');
    const suggestionsBox = document.getElementById('retro-suggestions');
    const raGameIdInput = document.getElementById('retroAchievementsGameId');
    const raConsoleIdInput = document.getElementById('retroConsoleId');
    const retroSelected = document.getElementById('retro-selected');
    const platformInput = document.getElementById('platform');

    if (!retroCheckbox || !retroSection) return;

    function toggleRetroSection() {
        retroSection.classList.toggle('visible', retroCheckbox.checked);
    }

    retroCheckbox.addEventListener('change', () => {
        toggleRetroSection();
        if (retroCheckbox.checked) {
            retroSearch?.focus();
        }
    });
    toggleRetroSection();

    if (!retroSearch || !suggestionsBox) return;

    let debounceTimer = null;

    retroSearch.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        const query = retroSearch.value.trim();
        if (query.length < 2) {
            suggestionsBox.innerHTML = '';
            suggestionsBox.classList.remove('visible');
            return;
        }
        debounceTimer = setTimeout(() => searchRetro(query), 350);
    });

    document.addEventListener('click', (e) => {
        if (!suggestionsBox.contains(e.target) && e.target !== retroSearch) {
            suggestionsBox.classList.remove('visible');
        }
    });

    function searchRetro(query) {
        fetch(`/games/search-retro?query=${encodeURIComponent(query)}`, { credentials: 'same-origin' })
            .then(r => r.ok ? r.json() : [])
            .then(results => renderSuggestions(results))
            .catch(() => suggestionsBox.classList.remove('visible'));
    }

    function renderSuggestions(results) {
        suggestionsBox.innerHTML = '';
        if (!results.length) {
            suggestionsBox.innerHTML = '<div class="suggestion-empty">Nenhum jogo retro encontrado no catálogo RA.</div>';
            suggestionsBox.classList.add('visible');
            return;
        }
        results.forEach(game => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'suggestion-item';
            btn.innerHTML = `<div class="suggestion-cover placeholder">🕹️</div>
                <div><strong>${escapeHtml(game.title)}</strong>
                <span class="meta">${escapeHtml(game.consoleName)} · RA #${game.raGameId}</span></div>`;
            btn.addEventListener('click', () => selectRetro(game));
            suggestionsBox.appendChild(btn);
        });
        suggestionsBox.classList.add('visible');
    }

    function selectRetro(game) {
        retroCheckbox.checked = true;
        toggleRetroSection();
        if (raGameIdInput) raGameIdInput.value = game.raGameId;
        if (raConsoleIdInput) raConsoleIdInput.value = game.consoleId;
        if (platformInput && !platformInput.value) platformInput.value = game.consoleName;
        const titleInput = document.getElementById('title');
        if (titleInput && !titleInput.value) titleInput.value = game.title;
        if (retroSelected) {
            retroSelected.textContent = `RetroAchievements: ${game.title} (ID ${game.raGameId})`;
            retroSelected.style.display = '';
        }
        retroSearch.value = game.title;
        suggestionsBox.classList.remove('visible');
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
});
