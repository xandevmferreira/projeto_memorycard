(function () {
    var el = document.getElementById('play-timer');
    if (!el) return;

    var startedAt = el.dataset.startedAt;
    if (!startedAt) return;

    var display = el.querySelector('.play-timer-value');
    if (!display) return;

    function tick() {
        var start = new Date(startedAt).getTime();
        var now = Date.now();
        var seconds = Math.max(0, Math.floor((now - start) / 1000));
        var h = Math.floor(seconds / 3600);
        var m = Math.floor((seconds % 3600) / 60);
        var s = seconds % 60;
        display.textContent =
            String(h).padStart(2, '0') + ':' +
            String(m).padStart(2, '0') + ':' +
            String(s).padStart(2, '0');
    }

    tick();
    setInterval(tick, 1000);
})();
