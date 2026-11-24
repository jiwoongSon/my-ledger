package com.myledger.web;

import com.myledger.domain.CompanionType;
import com.myledger.domain.RecurringTransaction;
import com.myledger.domain.TransactionType;
import com.myledger.dto.RecurringSummaryDto.BatchExecutionResult;
import com.myledger.dto.RecurringTransactionForm;
import com.myledger.dto.TransactionForm;
import com.myledger.service.CategoryService;
import com.myledger.service.RecurringTransactionScheduler;
import com.myledger.service.RecurringTransactionService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recurring")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringService;
    private final RecurringTransactionScheduler scheduler;
    private final CategoryService categoryService;
    private final Clock clock;

    public RecurringTransactionController(
            RecurringTransactionService recurringService,
            RecurringTransactionScheduler scheduler,
            CategoryService categoryService,
            Clock clock
    ) {
        this.recurringService = recurringService;
        this.scheduler = scheduler;
        this.categoryService = categoryService;
        this.clock = clock;
    }

    @GetMapping
    public String listRecurring(Model model) {
        LocalDate today = LocalDate.now(clock);

        model.addAttribute("activePage", "recurring");
        model.addAttribute("today", today);
        model.addAttribute("recurringList", recurringService.findAll());
        model.addAttribute("summary", recurringService.getSummary());
        model.addAttribute("categories", categoryService.findAllDtos());
        model.addAttribute("transactionTypes", TransactionType.values());
        model.addAttribute("companionTypes", CompanionType.values());

        if (!model.containsAttribute("recurringForm")) {
            model.addAttribute("recurringForm", RecurringTransactionForm.defaultForm(today.getDayOfMonth()));
        }
        if (!model.containsAttribute("transactionForm")) {
            model.addAttribute("transactionForm", TransactionForm.emptyForDate(today));
        }

        return "recurring";
    }

    @PostMapping
    public String createRecurring(
            @Valid @ModelAttribute("recurringForm") RecurringTransactionForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.recurringForm", bindingResult);
            redirectAttributes.addFlashAttribute("recurringForm", form);
            redirectAttributes.addFlashAttribute("openRecurringModal", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Please check the highlighted fields and try again.");
            return "redirect:/recurring";
        }

        try {
            recurringService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Recurring rule created successfully.");
            return "redirect:/recurring";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("recurringForm", form);
            redirectAttributes.addFlashAttribute("openRecurringModal", true);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recurring";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        RecurringTransaction existing = recurringService.getById(id);
        LocalDate today = LocalDate.now(clock);

        model.addAttribute("activePage", "recurring");
        model.addAttribute("recurringId", id);
        model.addAttribute("categories", categoryService.findAllDtos());
        model.addAttribute("transactionTypes", TransactionType.values());
        model.addAttribute("companionTypes", CompanionType.values());
        if (!model.containsAttribute("recurringForm")) {
            model.addAttribute("recurringForm", RecurringTransactionForm.fromEntity(existing));
        }
        if (!model.containsAttribute("transactionForm")) {
            model.addAttribute("transactionForm", TransactionForm.emptyForDate(today));
        }
        return "recurring-edit";
    }

    @PostMapping("/{id}")
    public String updateRecurring(
            @PathVariable Long id,
            @Valid @ModelAttribute("recurringForm") RecurringTransactionForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateEditModel(model, id);
            model.addAttribute("errorMessage", "Please check the highlighted fields and try again.");
            return "recurring-edit";
        }

        try {
            recurringService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Recurring rule updated successfully.");
            return "redirect:/recurring";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("categoryId", "invalid.category", ex.getMessage());
            populateEditModel(model, id);
            model.addAttribute("errorMessage", ex.getMessage());
            return "recurring-edit";
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggleRecurring(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        RecurringTransaction updated = recurringService.toggleActive(id);
        String msg = updated.isActive()
                ? "Recurring rule resumed (Active)."
                : "Recurring rule paused.";
        redirectAttributes.addFlashAttribute("successMessage", msg);
        return "redirect:/recurring";
    }

    @PostMapping("/{id}/delete")
    public String deleteRecurring(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        recurringService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Recurring rule deleted successfully.");
        return "redirect:/recurring";
    }

    @PostMapping("/run-now")
    public String runNow(RedirectAttributes redirectAttributes) {
        BatchExecutionResult result = scheduler.runDailyRecurringBatch();
        String message;
        if (result.dueCount() == 0) {
            message = String.format(
                    "Batch check complete for %s: No active recurring rules scheduled for Day %d.",
                    result.executionDate(),
                    result.executionDate().getDayOfMonth()
            );
        } else {
            message = String.format(
                    "Batch run complete for %s: %d transaction(s) recorded, %d skipped (already recorded this month).",
                    result.executionDate(),
                    result.createdCount(),
                    result.skippedCount()
            );
        }
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/recurring";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(NoSuchElementException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "The requested recurring rule could not be found.");
        return "redirect:/recurring";
    }

    private void populateEditModel(Model model, Long id) {
        LocalDate today = LocalDate.now(clock);
        model.addAttribute("activePage", "recurring");
        model.addAttribute("recurringId", id);
        model.addAttribute("categories", categoryService.findAllDtos());
        model.addAttribute("transactionTypes", TransactionType.values());
        model.addAttribute("companionTypes", CompanionType.values());
        model.addAttribute("transactionForm", TransactionForm.emptyForDate(today));
    }
}
