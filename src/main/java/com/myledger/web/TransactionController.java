package com.myledger.web;

import com.myledger.domain.CompanionType;
import com.myledger.domain.Transaction;
import com.myledger.domain.TransactionType;
import com.myledger.dto.TransactionForm;
import com.myledger.dto.TransactionSummaryDto;
import com.myledger.service.CategoryService;
import com.myledger.service.CsvExportService;
import com.myledger.service.TransactionService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private static final DateTimeFormatter MONTH_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final CsvExportService csvExportService;
    private final Clock clock;

    public TransactionController(
            TransactionService transactionService,
            CategoryService categoryService,
            CsvExportService csvExportService,
            Clock clock
    ) {
        this.transactionService = transactionService;
        this.categoryService = categoryService;
        this.csvExportService = csvExportService;
        this.clock = clock;
    }

    @GetMapping
    public String listTransactions(
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "false") boolean allMonths,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) CompanionType companionType,
            @RequestParam(required = false) Boolean fixedOnly,
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        LocalDate today = LocalDate.now(clock);
        YearMonth selectedMonth = parseYearMonthOrDefault(month, YearMonth.from(today));

        LocalDate startDate = allMonths ? null : selectedMonth.atDay(1);
        LocalDate endDate = allMonths ? null : selectedMonth.atEndOfMonth();

        List<Transaction> transactions = transactionService.findFiltered(
                startDate, endDate, type, categoryId, companionType, fixedOnly, keyword
        );
        TransactionSummaryDto summary = transactionService.summarizeFiltered(
                startDate, endDate, type, categoryId, companionType, fixedOnly, keyword
        );

        model.addAttribute("activePage", "transactions");
        model.addAttribute("today", today);
        model.addAttribute("selectedMonth", selectedMonth.toString());
        model.addAttribute("selectedMonthLabel", allMonths ? "All Time" : selectedMonth.format(MONTH_LABEL_FORMATTER));
        model.addAttribute("prevMonth", selectedMonth.minusMonths(1).toString());
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1).toString());
        model.addAttribute("allMonths", allMonths);
        model.addAttribute("filterType", type);
        model.addAttribute("filterCategoryId", categoryId);
        model.addAttribute("filterCompanionType", companionType);
        model.addAttribute("filterFixedOnly", Boolean.TRUE.equals(fixedOnly));
        model.addAttribute("filterKeyword", keyword != null ? keyword : "");

        model.addAttribute("transactions", transactions);
        model.addAttribute("summary", summary);
        model.addAttribute("categories", categoryService.findAllDtos());
        model.addAttribute("transactionTypes", TransactionType.values());
        model.addAttribute("companionTypes", CompanionType.values());

        if (!model.containsAttribute("transactionForm")) {
            model.addAttribute("transactionForm", TransactionForm.emptyForDate(today));
        }

        return "transactions";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "false") boolean allMonths,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) CompanionType companionType,
            @RequestParam(required = false) Boolean fixedOnly,
            @RequestParam(required = false) String keyword
    ) {
        LocalDate effectiveStart = startDate;
        LocalDate effectiveEnd = endDate;
        String fileSuffix;

        if (effectiveStart != null || effectiveEnd != null) {
            fileSuffix = (effectiveStart != null ? effectiveStart : "start")
                    + "_to_"
                    + (effectiveEnd != null ? effectiveEnd : "end");
        } else if (allMonths) {
            fileSuffix = "all-time";
        } else {
            YearMonth selectedMonth = parseYearMonthOrDefault(month, YearMonth.from(LocalDate.now(clock)));
            effectiveStart = selectedMonth.atDay(1);
            effectiveEnd = selectedMonth.atEndOfMonth();
            fileSuffix = selectedMonth.toString();
        }

        List<Transaction> transactions = transactionService.findFiltered(
                effectiveStart, effectiveEnd, type, categoryId, companionType, fixedOnly, keyword
        );
        byte[] csvBytes = csvExportService.exportTransactions(transactions);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("myledger-transactions-" + fileSuffix + ".csv", StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok().headers(headers).body(csvBytes);
    }

    @PostMapping
    public String createTransaction(
            @Valid @ModelAttribute("transactionForm") TransactionForm form,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "/transactions") String returnTo,
            RedirectAttributes redirectAttributes
    ) {
        String safeReturn = sanitizeReturnPath(returnTo);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.transactionForm", bindingResult);
            redirectAttributes.addFlashAttribute("transactionForm", form);
            redirectAttributes.addFlashAttribute("openQuickAdd", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Please check the highlighted fields and try again.");
            return "redirect:" + safeReturn;
        }

        try {
            Transaction saved = transactionService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Transaction added successfully.");
            if (safeReturn.equals("/transactions") && saved.getTransactionDate() != null) {
                YearMonth ym = YearMonth.from(saved.getTransactionDate());
                return "redirect:/transactions?month=" + ym;
            }
            return "redirect:" + safeReturn;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("transactionForm", form);
            redirectAttributes.addFlashAttribute("openQuickAdd", true);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + safeReturn;
        }
    }

    @GetMapping("/{id}/edit")
    public String editTransactionForm(
            @PathVariable Long id,
            @RequestParam(required = false) String month,
            Model model
    ) {
        Transaction transaction = transactionService.getById(id);
        model.addAttribute("activePage", "transactions");
        model.addAttribute("transactionId", id);
        model.addAttribute("returnMonth", month != null ? month : YearMonth.from(transaction.getTransactionDate()).toString());
        model.addAttribute("categories", categoryService.findAllDtos());
        model.addAttribute("transactionTypes", TransactionType.values());
        model.addAttribute("companionTypes", CompanionType.values());
        if (!model.containsAttribute("transactionForm")) {
            model.addAttribute("transactionForm", TransactionForm.fromEntity(transaction));
        }
        return "transaction-edit";
    }

    @PostMapping("/{id}")
    public String updateTransaction(
            @PathVariable Long id,
            @Valid @ModelAttribute("transactionForm") TransactionForm form,
            BindingResult bindingResult,
            @RequestParam(required = false) String returnMonth,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateEditModel(model, id, returnMonth);
            model.addAttribute("errorMessage", "Please check the highlighted fields and try again.");
            return "transaction-edit";
        }

        try {
            Transaction updated = transactionService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Transaction updated successfully.");
            String targetMonth = (returnMonth != null && !returnMonth.isBlank())
                    ? returnMonth
                    : YearMonth.from(updated.getTransactionDate()).toString();
            return "redirect:/transactions?month=" + targetMonth;
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("categoryId", "invalid.category", ex.getMessage());
            populateEditModel(model, id, returnMonth);
            model.addAttribute("errorMessage", ex.getMessage());
            return "transaction-edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteTransaction(
            @PathVariable Long id,
            @RequestParam(defaultValue = "/transactions") String returnTo,
            RedirectAttributes redirectAttributes
    ) {
        transactionService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Transaction deleted successfully.");
        return "redirect:" + sanitizeReturnPath(returnTo);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(NoSuchElementException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "The requested transaction could not be found.");
        return "redirect:/transactions";
    }

    private void populateEditModel(Model model, Long id, String returnMonth) {
        model.addAttribute("activePage", "transactions");
        model.addAttribute("transactionId", id);
        model.addAttribute("returnMonth", returnMonth != null ? returnMonth : "");
        model.addAttribute("categories", categoryService.findAllDtos());
        model.addAttribute("transactionTypes", TransactionType.values());
        model.addAttribute("companionTypes", CompanionType.values());
    }

    private static YearMonth parseYearMonthOrDefault(String value, YearMonth fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }

    private static String sanitizeReturnPath(String path) {
        if (path == null || path.isBlank() || !path.startsWith("/") || path.startsWith("//")) {
            return "/transactions";
        }
        return path;
    }
}
