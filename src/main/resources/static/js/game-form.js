document.addEventListener('DOMContentLoaded', () => {
    const statusSelect = document.getElementById('status');
    const completedAtInput = document.getElementById('completedAt');
    const completedGroup = completedAtInput?.closest('.form-group');

    if (completedAtInput && statusSelect) {
        if (completedAtInput.value && statusSelect.value !== 'COMPLETED') {
            statusSelect.value = 'COMPLETED';
        }

        completedAtInput.addEventListener('change', () => {
            if (completedAtInput.value) {
                statusSelect.value = 'COMPLETED';
        updateCompletedVisibility();
    }

    const retroCheckbox = document.getElementById('retro');
    const retroSection = document.getElementById('retro-section');
    if (retroCheckbox && retroSection) {
        retroSection.classList.toggle('visible', retroCheckbox.checked);
    }
});

        statusSelect.addEventListener('change', () => {
            if (statusSelect.value === 'COMPLETED' && !completedAtInput.value) {
                completedAtInput.value = new Date().toISOString().slice(0, 10);
            }
            if (statusSelect.value !== 'COMPLETED') {
                completedAtInput.value = '';
            }
            updateCompletedVisibility();
        });

        updateCompletedVisibility();
    }

    function updateCompletedVisibility() {
        if (!completedGroup) return;
        const show = statusSelect.value === 'COMPLETED';
        completedGroup.style.display = show ? '' : 'none';
    }
});
