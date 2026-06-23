document.querySelectorAll('.spoiler-reveal').forEach(function (btn) {
    btn.addEventListener('click', function () {
        var block = btn.closest('.spoiler-hidden');
        if (!block) return;
        var content = block.querySelector('.spoiler-content');
        if (content) {
            content.classList.remove('hidden');
        }
        btn.style.display = 'none';
        var warning = block.querySelector('.spoiler-warning');
        if (warning) {
            warning.textContent = '⚠️ Spoiler';
        }
    });
});
