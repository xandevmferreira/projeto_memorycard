(function () {
    const stage = document.getElementById('tape-stage');
    const content = document.getElementById('tape-content');
    if (!stage || !content) {
        return;
    }
    stage.classList.add('tape-insert-entering');
    requestAnimationFrame(function () {
        content.classList.add('tape-insert-visible');
    });
})();
