(function () {
    const preset = document.getElementById('platform-preset');
    const input = document.getElementById('platform');
    if (!preset || !input) {
        return;
    }

    function syncFromPreset() {
        const value = preset.value;
        if (!value || value === '__custom__') {
            if (value === '__custom__') {
                input.focus();
            }
            return;
        }
        input.value = value;
        input.dispatchEvent(new Event('change', { bubbles: true }));
    }

    function syncPresetFromInput() {
        const current = (input.value || '').trim();
        if (!current) {
            preset.value = '';
            return;
        }
        let matched = false;
        for (const opt of preset.options) {
            if (opt.value && opt.value !== '__custom__'
                && opt.value.toLowerCase() === current.toLowerCase()) {
                preset.value = opt.value;
                matched = true;
                break;
            }
        }
        if (!matched) {
            preset.value = '__custom__';
        }
    }

    preset.addEventListener('change', syncFromPreset);
    input.addEventListener('blur', syncPresetFromInput);
    syncPresetFromInput();
})();
