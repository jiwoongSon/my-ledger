(function () {
    'use strict';

    const FOOD_CAFE_NAMES = new Set(['Food', 'Cafe & Leisure']);

    function formatKrw(raw) {
        const cleaned = String(raw || '').replace(/[^\d]/g, '');
        if (!cleaned) return '';
        return '₩' + Number(cleaned).toLocaleString('en-US');
    }

    function initTransactionForm(form) {
        if (!form || form.dataset.initialized === 'true') return;
        form.dataset.initialized = 'true';

        const catEl = document.getElementById('myledger-categories-json');
        const categories = catEl ? JSON.parse(catEl.textContent || '[]') : (window.MYLEDGER_CATEGORIES || []);
        const typeInput = form.querySelector('[data-role="type-input"]');
        const typeButtons = form.querySelectorAll('[data-role="type-toggle"]');
        const categorySelect = form.querySelector('[data-role="category-select"]');
        const expenseOnlySection = form.querySelector('[data-role="expense-only"]');
        const companionInputs = form.querySelectorAll('input[name="companionType"]');
        const fixedCheckbox = form.querySelector('input[name="fixed"]');
        const foodHint = form.querySelector('[data-role="food-companion-hint"]');
        const amountInput = form.querySelector('input[name="amount"]');
        const amountPreview = form.querySelector('[data-role="amount-preview"]');

        function populateCategories(selectedType, preferredId) {
            if (!categorySelect) return;
            const currentVal = preferredId !== undefined ? String(preferredId || '') : categorySelect.value;
            const filtered = categories.filter((c) => c.type === selectedType);

            categorySelect.innerHTML = '';
            const placeholder = document.createElement('option');
            placeholder.value = '';
            placeholder.textContent = selectedType === 'INCOME'
                ? 'Select an income category'
                : 'Select an expense category';
            categorySelect.appendChild(placeholder);

            let matched = false;
            filtered.forEach((cat) => {
                const option = document.createElement('option');
                option.value = String(cat.id);
                option.textContent = cat.name;
                option.dataset.name = cat.name;
                if (String(cat.id) === currentVal) {
                    option.selected = true;
                    matched = true;
                }
                categorySelect.appendChild(option);
            });

            if (!matched && filtered.length > 0 && !currentVal) {
                // Keep placeholder selected unless editing
                categorySelect.selectedIndex = 0;
            }
            updateFoodHint();
        }

        function updateFoodHint() {
            if (!foodHint || !categorySelect) return;
            const selectedOption = categorySelect.options[categorySelect.selectedIndex];
            const catName = selectedOption ? selectedOption.dataset.name : '';
            if (FOOD_CAFE_NAMES.has(catName)) {
                foodHint.classList.remove('hidden');
            } else {
                foodHint.classList.add('hidden');
            }
        }

        function applyTypeState(selectedType, preferredCategoryId) {
            if (typeInput) {
                typeInput.value = selectedType;
            }

            typeButtons.forEach((btn) => {
                const isActive = btn.dataset.type === selectedType;
                btn.setAttribute('aria-pressed', String(isActive));
                if (isActive) {
                    if (selectedType === 'EXPENSE') {
                        btn.className = 'flex-1 py-2.5 px-4 text-sm font-semibold rounded-lg transition bg-rose-600 text-white shadow-sm';
                    } else {
                        btn.className = 'flex-1 py-2.5 px-4 text-sm font-semibold rounded-lg transition bg-emerald-600 text-white shadow-sm';
                    }
                } else {
                    btn.className = 'flex-1 py-2.5 px-4 text-sm font-semibold rounded-lg transition text-slate-600 hover:text-slate-900 hover:bg-slate-200/60';
                }
            });

            if (expenseOnlySection) {
                if (selectedType === 'INCOME') {
                    expenseOnlySection.classList.add('hidden');
                    companionInputs.forEach((radio) => {
                        radio.checked = radio.value === 'NONE';
                    });
                    if (fixedCheckbox) {
                        fixedCheckbox.checked = false;
                    }
                } else {
                    expenseOnlySection.classList.remove('hidden');
                }
            }

            populateCategories(selectedType, preferredCategoryId);
        }

        typeButtons.forEach((btn) => {
            btn.addEventListener('click', () => {
                applyTypeState(btn.dataset.type, '');
            });
        });

        if (categorySelect) {
            categorySelect.addEventListener('change', updateFoodHint);
        }

        if (amountInput && amountPreview) {
            const syncPreview = () => {
                const formatted = formatKrw(amountInput.value);
                amountPreview.textContent = formatted ? formatted + ' KRW' : '';
            };
            amountInput.addEventListener('input', syncPreview);
            syncPreview();
        }

        const initialType = (typeInput && typeInput.value) ? typeInput.value : 'EXPENSE';
        const initialCategoryId = categorySelect ? categorySelect.dataset.selectedId || categorySelect.value : '';
        applyTypeState(initialType, initialCategoryId);
    }

    function initQuickAddModal() {
        const modal = document.getElementById('quickAddModal');
        if (!modal) return;

        const openModal = () => {
            modal.classList.remove('hidden');
            modal.classList.add('flex');
            document.body.classList.add('overflow-hidden');
            const amountInput = modal.querySelector('input[name="amount"]');
            if (amountInput) {
                setTimeout(() => amountInput.focus(), 60);
            }
        };

        const closeModal = () => {
            modal.classList.add('hidden');
            modal.classList.remove('flex');
            document.body.classList.remove('overflow-hidden');
        };

        document.querySelectorAll('[data-open-quick-add]').forEach((trigger) => {
            trigger.addEventListener('click', (e) => {
                e.preventDefault();
                openModal();
            });
        });

        modal.querySelectorAll('[data-close-quick-add]').forEach((closer) => {
            closer.addEventListener('click', closeModal);
        });

        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeModal();
            }
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && !modal.classList.contains('hidden')) {
                closeModal();
            }
        });

        if (modal.dataset.autoOpen === 'true') {
            openModal();
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('[data-transaction-form]').forEach(initTransactionForm);
        initQuickAddModal();
    });
})();
