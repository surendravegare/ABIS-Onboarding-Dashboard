/* Quick Statistics widget — purely a renderer.
   All counting/grouping happens in the database (see StudentRepository's
   countGroupedByDomain / countGroupedByCourse queries). This file just
   takes the numbers already computed there (window.QUICK_STATS) and
   draws the bar chart, donut chart and legend for whichever field
   (Domain / Course) the user selects. */
(function () {

    const PALETTE = [
        '#1e3a8a', '#2563eb', '#0ea5e9', '#059669',
        '#b45309', '#dc2626', '#7c3aed', '#db2777',
        '#0d9488', '#ca8a04'
    ];

    let barChart = null;
    let donutChart = null;

    function renderLegend(labels, counts, colors) {
        const legend = document.getElementById('statLegend');
        legend.innerHTML = '';

        labels.forEach(function (label, i) {
            const li = document.createElement('li');

            const dot = document.createElement('span');
            dot.className = 'dot';
            dot.style.background = colors[i % colors.length];

            const text = document.createElement('span');
            text.textContent = label;

            const n = document.createElement('span');
            n.className = 'n';
            n.textContent = counts[i];

            li.appendChild(dot);
            li.appendChild(text);
            li.appendChild(n);
            legend.appendChild(li);
        });
    }

    function showEmptyState() {
        document.querySelector('.stats-charts').innerHTML =
            '<p class="stats-empty">No student records yet — add a student to see statistics.</p>';
    }

    function render(field) {
        const stats = (window.QUICK_STATS || {})[field];

        if (!stats || !stats.labels || stats.labels.length === 0) {
            showEmptyState();
            return;
        }

        const labels = stats.labels;
        const counts = stats.counts;
        const colors = labels.map(function (_, i) { return PALETTE[i % PALETTE.length]; });

        const barCtx = document.getElementById('statBarChart').getContext('2d');
        const donutCtx = document.getElementById('statDonutChart').getContext('2d');

        if (barChart) barChart.destroy();
        if (donutChart) donutChart.destroy();

        barChart = new Chart(barCtx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Students',
                    data: counts,
                    backgroundColor: colors,
                    borderRadius: 6,
                    maxBarThickness: 28
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, ticks: { precision: 0, font: { size: 11 } } },
                    x: { ticks: { font: { size: 10 } } }
                }
            }
        });

        donutChart = new Chart(donutCtx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: counts,
                    backgroundColor: colors,
                    borderWidth: 2,
                    borderColor: '#ffffff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '65%',
                plugins: { legend: { display: false } }
            }
        });

        renderLegend(labels, counts, colors);
    }

    document.addEventListener('DOMContentLoaded', function () {
        const select = document.getElementById('statField');
        render(select.value);

        select.addEventListener('change', function () {
            render(select.value);
        });
    });

})();
