package com.myledger.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myledger.domain.Category;
import com.myledger.domain.Transaction;
import com.myledger.repository.CategoryRepository;
import com.myledger.repository.TransactionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    void redirectsUnauthenticatedUsersToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/transactions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void allowsAuthenticatedUserToCreateUpdateAndDeleteTransaction() throws Exception {
        Category food = categoryRepository.findByName("Food").orElseThrow();
        Category cafe = categoryRepository.findByName("Cafe & Leisure").orElseThrow();

        // Create
        mockMvc.perform(post("/transactions")
                        .with(user("testadmin").roles("OWNER"))
                        .with(csrf())
                        .param("type", "EXPENSE")
                        .param("categoryId", food.getId().toString())
                        .param("amount", "14500")
                        .param("transactionDate", "2026-11-24")
                        .param("transactionTime", "12:30")
                        .param("companionType", "ALONE")
                        .param("fixed", "false")
                        .param("memo", "Bibimbap lunch"))
                .andExpect(status().is3xxRedirection());

        List<Transaction> saved = transactionRepository.findAll();
        assertThat(saved).hasSize(1);
        Long txId = saved.get(0).getId();
        assertThat(saved.get(0).getAmount()).isEqualByComparingTo("14500");

        // Verify analytics JSON endpoint reflects saved transaction
        mockMvc.perform(get("/api/analytics")
                        .with(user("testadmin").roles("OWNER"))
                        .param("period", "MONTHLY")
                        .param("date", "2026-11-24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(14500))
                .andExpect(jsonPath("$.foodCafeCompanion.aloneAmount").value(14500));

        // Update
        mockMvc.perform(post("/transactions/" + txId)
                        .with(user("testadmin").roles("OWNER"))
                        .with(csrf())
                        .param("type", "EXPENSE")
                        .param("categoryId", cafe.getId().toString())
                        .param("amount", "19000")
                        .param("transactionDate", "2026-11-24")
                        .param("companionType", "WITH_OTHERS")
                        .param("fixed", "false")
                        .param("memo", "Coffee and cake"))
                .andExpect(status().is3xxRedirection());

        Transaction updated = transactionRepository.findWithCategoryById(txId).orElseThrow();
        assertThat(updated.getCategory().getName()).isEqualTo("Cafe & Leisure");
        assertThat(updated.getAmount()).isEqualByComparingTo("19000");

        // Delete
        mockMvc.perform(post("/transactions/" + txId + "/delete")
                        .with(user("testadmin").roles("OWNER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void rejectsCategoryTypeMismatchAndNonPositiveAmount() throws Exception {
        Category salary = categoryRepository.findByName("Salary").orElseThrow();

        // Mismatch: EXPENSE with INCOME category (Salary)
        mockMvc.perform(post("/transactions")
                        .with(user("testadmin").roles("OWNER"))
                        .with(csrf())
                        .param("type", "EXPENSE")
                        .param("categoryId", salary.getId().toString())
                        .param("amount", "50000")
                        .param("transactionDate", "2026-11-24"))
                .andExpect(status().is3xxRedirection());

        // Non-positive amount
        Category food = categoryRepository.findByName("Food").orElseThrow();
        mockMvc.perform(post("/transactions")
                        .with(user("testadmin").roles("OWNER"))
                        .with(csrf())
                        .param("type", "EXPENSE")
                        .param("categoryId", food.getId().toString())
                        .param("amount", "0")
                        .param("transactionDate", "2026-11-24"))
                .andExpect(status().is3xxRedirection());

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void rendersDashboardAndTransactionsPagesSuccessfully() throws Exception {
        mockMvc.perform(get("/").with(user("testadmin").roles("OWNER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions").with(user("testadmin").roles("OWNER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }
}
