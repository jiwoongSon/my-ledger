(function () {
    'use strict';

    const CATEGORY_PALETTE = [
        '#4f46e5', '#0ea5e9', '#10b981', '#f59e0b', '#f43f5e',
        '#8b5cf6', '#14b8a6', '#ec4899', '#64748b', '#eab308'
    ];

    const charts = {
        category: null,
        companion: null,
        fixed: null,
        trend: null
    };

    let currentState = {
        period: 'MONTHLY',
        date: null
    };

    function formatKrw(value) {
        const num = Number(value || 0);
        const absFormatted = Math.abs(num).toLocaleString('en-US', { maximumFractionDigits: 0 });
        return num < 0 ? '-₩' + absFormatted : '₩' + absFormatted;
    }

    function formatSignedKrw(value) {
        const num = Number(value || 0);
        const absFormatted = Math.abs(num).toLocaleString('en-US', { maximumFractionDigits: 0 });
        if (num > 0) return '+₩' + absFormatted;
        if (num < 0) return '-₩' + absFormatted;
        return '₩0';
    }

    function formatPercent(value) {
        if (value === null || value === undefined) return 'N/A';
        return Number(value).toFixed(1) + '%';
    }

    function setElText(id, text) {
        const el = document.getElementById(id);
        if (el) el.textContent = text;
    }

    function toggleEmptyState(chartCanvasId, emptyId, hasData) {
        const canvas = document.getElementById(chartCanvasId);
        const empty = document.getElementById(emptyId);
        if (!canvas || !empty) return;
        if (hasData) {
            canvas.classList.remove('hidden');
            empty.classList.add('hidden');
        } else {
            canvas.classList.add('hidden');
            empty.classList.remove('hidden');
        }
    }

    function destroyChart(key) {
        if (charts[key]) {
            charts[key].destroy();
            charts[key] = null;
        }
    }

    function renderKpis(data) {
        setElText('periodLabelText', data.periodLabel);
        setElText('periodRangeText', data.startDate === data.endDate
            ? data.startDate
            : data.startDate + ' to ' + data.endDate);

        setElText('kpiTotalIncome', formatKrw(data.totalIncome));
        setElText('kpiTotalExpense', formatKrw(data.totalExpense));

        const netEl = document.getElementById('kpiNetBalance');
        if (netEl) {
            netEl.textContent = formatSignedKrw(data.netBalance);
            netEl.classList.remove('text-emerald-600', 'text-rose-600', 'text-slate-900');
            const netNum = Number(data.netBalance || 0);
            if (netNum > 0) netEl.classList.add('text-emerald-600');
            else if (netNum < 0) netEl.classList.add('text-rose-600');
            else netEl.classList.add('text-slate-900');
        }

        const savingsEl = document.getElementById('kpiSavingsRate');
        const savingsSubEl = document.getElementById('kpiSavingsSubtext');
        if (savingsEl) {
            if (data.savingsRate === null || data.savingsRate === undefined) {
                savingsEl.textContent = 'N/A';
                if (savingsSubEl) savingsSubEl.textContent = 'No income in selected period';
            } else {
                savingsEl.textContent = formatPercent(data.savingsRate);
                if (savingsSubEl) {
                    const rate = Number(data.savingsRate);
                    savingsSubEl.textContent = rate >= 30
                        ? 'Healthy savings pace'
                        : rate >= 0
                            ? 'Positive net savings'
                            : 'Spending exceeded income';
                }
            }
        }

        const dateInput = document.getElementById('analyticsDatePicker');
        if (dateInput && data.anchorDate) {
            dateInput.value = data.anchorDate;
        }

        const prevBtn = document.getElementById('periodPrevBtn');
        const nextBtn = document.getElementById('periodNextBtn');
        if (prevBtn) prevBtn.dataset.targetDate = data.prevDate;
        if (nextBtn) nextBtn.dataset.targetDate = data.nextDate;

        const exportBtn = document.getElementById('dashboardExportCsvBtn');
        if (exportBtn && data.startDate && data.endDate) {
            const exportParams = new URLSearchParams();
            exportParams.set('startDate', data.startDate);
            exportParams.set('endDate', data.endDate);
            exportBtn.href = '/transactions/export?' + exportParams.toString();
        }

        document.querySelectorAll('[data-period-tab]').forEach((tab) => {
            const active = tab.dataset.periodTab === data.period;
            tab.className = active
                ? 'px-3.5 py-1.5 text-sm font-semibold rounded-lg bg-slate-900 text-white shadow-sm transition'
                : 'px-3.5 py-1.5 text-sm font-medium rounded-lg text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition';
        });
    }

    function renderCategoryChart(items) {
        destroyChart('category');
        const hasData = Array.isArray(items) && items.length > 0;
        toggleEmptyState('categoryChart', 'categoryChartEmpty', hasData);

        const listEl = document.getElementById('categoryBreakdownList');
        if (listEl) {
            listEl.innerHTML = '';
            if (hasData) {
                items.slice(0, 6).forEach((item, idx) => {
                    const color = CATEGORY_PALETTE[idx % CATEGORY_PALETTE.length];
                    const row = document.createElement('div');
                    row.className = 'flex items-center justify-between text-xs py-1';
                    row.innerHTML = `
                        <div class="flex items-center gap-2 min-w-0">
                            <span class="w-2.5 h-2.5 rounded-full shrink-0" style="background-color:${color}"></span>
                            <span class="font-medium text-slate-700 truncate"></span>
                            <span class="text-slate-400 shrink-0">(${item.count})</span>
                        </div>
                        <div class="flex items-center gap-2 shrink-0 tabular-nums">
                            <span class="font-semibold text-slate-900">${formatKrw(item.amount)}</span>
                            <span class="text-slate-500 w-11 text-right">${formatPercent(item.percentage)}</span>
                        </div>
                    `;
                    row.querySelector('.truncate').textContent = item.categoryName;
                    listEl.appendChild(row);
                });
            }
        }

        if (!hasData) return;

        const ctx = document.getElementById('categoryChart').getContext('2d');
        charts.category = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: items.map((i) => i.categoryName),
                datasets: [{
                    data: items.map((i) => Number(i.amount)),
                    backgroundColor: items.map((_, idx) => CATEGORY_PALETTE[idx % CATEGORY_PALETTE.length]),
                    borderWidth: 2,
                    borderColor: '#ffffff',
                    hoverOffset: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '64%',
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: (context) => {
                                const item = items[context.dataIndex];
                                return ` ${item.categoryName}: ${formatKrw(item.amount)} (${formatPercent(item.percentage)})`;
                            }
                        }
                    }
                }
            }
        });
    }

    function renderCompanionChart(comp) {
        destroyChart('companion');
        const alone = Number(comp.aloneAmount || 0);
        const withOthers = Number(comp.withOthersAmount || 0);
        const unspecified = Number(comp.unspecifiedAmount || 0);
        const hasTagged = (alone + withOthers) > 0;

        setElText('companionAloneAmount', formatKrw(alone));
        setElText('companionAloneMeta', `${comp.aloneCount || 0} entries · ${formatPercent(comp.alonePercentage)}`);
        setElText('companionOthersAmount', formatKrw(withOthers));
        setElText('companionOthersMeta', `${comp.withOthersCount || 0} entries · ${formatPercent(comp.withOthersPercentage)}`);

        const untaggedNote = document.getElementById('companionUntaggedNote');
        if (untaggedNote) {
            if (unspecified > 0) {
                untaggedNote.textContent = `Untagged (N/A): ${formatKrw(unspecified)} (${comp.unspecifiedCount} entries)`;
                untaggedNote.classList.remove('hidden');
            } else {
                untaggedNote.classList.add('hidden');
            }
        }

        toggleEmptyState('companionChart', 'companionChartEmpty', hasTagged);
        if (!hasTagged) return;

        const ctx = document.getElementById('companionChart').getContext('2d');
        charts.companion = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Alone', 'With Others'],
                datasets: [{
                    label: 'Spending (KRW)',
                    data: [alone, withOthers],
                    backgroundColor: ['#0ea5e9', '#f59e0b'],
                    borderRadius: 10,
                    maxBarThickness: 56
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: (context) => ` ${context.label}: ${formatKrw(context.parsed.y)}`
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: { color: '#f1f5f9' },
                        ticks: {
                            color: '#64748b',
                            callback: (val) => formatKrw(val)
                        }
                    },
                    x: {
                        grid: { display: false },
                        ticks: { color: '#334155', font: { weight: '600' } }
                    }
                }
            }
        });
    }

    function renderFixedVariableChart(split) {
        destroyChart('fixed');
        const fixedAmt = Number(split.fixedAmount || 0);
        const variableAmt = Number(split.variableAmount || 0);
        const hasData = (fixedAmt + variableAmt) > 0;

        setElText('fixedAmountText', formatKrw(fixedAmt));
        setElText('fixedMetaText', `${split.fixedCount || 0} entries · ${formatPercent(split.fixedPercentage)}`);
        setElText('variableAmountText', formatKrw(variableAmt));
        setElText('variableMetaText', `${split.variableCount || 0} entries · ${formatPercent(split.variablePercentage)}`);

        toggleEmptyState('fixedChart', 'fixedChartEmpty', hasData);
        if (!hasData) return;

        const ctx = document.getElementById('fixedChart').getContext('2d');
        charts.fixed = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Fixed Expense', 'Variable Expense'],
                datasets: [{
                    data: [fixedAmt, variableAmt],
                    backgroundColor: ['#6366f1', '#fb7185'],
                    borderWidth: 2,
                    borderColor: '#ffffff',
                    hoverOffset: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '64%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            usePointStyle: true,
                            padding: 16,
                            color: '#334155'
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: (context) => {
                                const pct = context.dataIndex === 0 ? split.fixedPercentage : split.variablePercentage;
                                return ` ${context.label}: ${formatKrw(context.parsed)} (${formatPercent(pct)})`;
                            }
                        }
                    }
                }
            }
        });
    }

    function renderTrendChart(items) {
        destroyChart('trend');
        const hasAny = Array.isArray(items) && items.some((i) => Number(i.income) > 0 || Number(i.expense) > 0);
        toggleEmptyState('trendChart', 'trendChartEmpty', hasAny);
        if (!hasAny) return;

        const ctx = document.getElementById('trendChart').getContext('2d');
        charts.trend = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: items.map((i) => i.label),
                datasets: [
                    {
                        type: 'bar',
                        label: 'Income',
                        data: items.map((i) => Number(i.income)),
                        backgroundColor: 'rgba(16, 185, 129, 0.82)',
                        borderRadius: 6,
                        maxBarThickness: 22
                    },
                    {
                        type: 'bar',
                        label: 'Expense',
                        data: items.map((i) => Number(i.expense)),
                        backgroundColor: 'rgba(244, 63, 94, 0.82)',
                        borderRadius: 6,
                        maxBarThickness: 22
                    },
                    {
                        type: 'line',
                        label: 'Net Balance',
                        data: items.map((i) => Number(i.net)),
                        borderColor: '#4f46e5',
                        backgroundColor: '#4f46e5',
                        borderWidth: 2,
                        pointRadius: 3,
                        tension: 0.3,
                        fill: false
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                plugins: {
                    legend: {
                        position: 'top',
                        align: 'end',
                        labels: { usePointStyle: true, color: '#334155', boxWidth: 8 }
                    },
                    tooltip: {
                        callbacks: {
                            label: (context) => ` ${context.dataset.label}: ${formatKrw(context.parsed.y)}`
                        }
                    }
                },
                scales: {
                    y: {
                        grid: { color: '#f1f5f9' },
                        ticks: {
                            color: '#64748b',
                            callback: (val) => formatKrw(val)
                        }
                    },
                    x: {
                        grid: { display: false },
                        ticks: { color: '#64748b' }
                    }
                }
            }
        });
    }

    function renderAll(data) {
        currentState.period = data.period;
        currentState.date = data.anchorDate;
        renderKpis(data);
        renderCategoryChart(data.expenseByCategory || []);
        renderCompanionChart(data.foodCafeCompanion || {});
        renderFixedVariableChart(data.fixedVsVariable || {});
        renderTrendChart(data.monthlyTrend || []);
    }

    async function loadAnalytics(period, date) {
        const params = new URLSearchParams();
        params.set('period', period);
        if (date) params.set('date', date);

        try {
            const resp = await fetch('/api/analytics?' + params.toString(), {
                headers: { 'Accept': 'application/json' }
            });
            if (!resp.ok) {
                if (resp.status === 401 || resp.redirected) {
                    window.location.href = '/login';
                    return;
                }
                throw new Error('Failed to load analytics data.');
            }
            const data = await resp.json();
            renderAll(data);

            const url = new URL(window.location.href);
            url.searchParams.set('period', data.period);
            url.searchParams.set('date', data.anchorDate);
            window.history.replaceState({}, '', url.toString());
        } catch (err) {
            console.error(err);
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        const initialEl = document.getElementById('myledger-analytics-json');
        const initialData = initialEl ? JSON.parse(initialEl.textContent || 'null') : window.INITIAL_ANALYTICS;
        if (!initialData) return;
        renderAll(initialData);

        document.querySelectorAll('[data-period-tab]').forEach((tab) => {
            tab.addEventListener('click', () => {
                loadAnalytics(tab.dataset.periodTab, currentState.date);
            });
        });

        const prevBtn = document.getElementById('periodPrevBtn');
        if (prevBtn) {
            prevBtn.addEventListener('click', () => {
                loadAnalytics(currentState.period, prevBtn.dataset.targetDate);
            });
        }

        const nextBtn = document.getElementById('periodNextBtn');
        if (nextBtn) {
            nextBtn.addEventListener('click', () => {
                loadAnalytics(currentState.period, nextBtn.dataset.targetDate);
            });
        }

        const todayBtn = document.getElementById('periodTodayBtn');
        if (todayBtn) {
            todayBtn.addEventListener('click', () => {
                loadAnalytics(currentState.period, todayBtn.dataset.today);
            });
        }

        const datePicker = document.getElementById('analyticsDatePicker');
        if (datePicker) {
            datePicker.addEventListener('change', () => {
                if (datePicker.value) {
                    loadAnalytics(currentState.period, datePicker.value);
                }
            });
        }
    });
})();
