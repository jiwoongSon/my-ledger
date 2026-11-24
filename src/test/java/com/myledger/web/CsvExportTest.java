package com.myledger.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myledger.domain.Category;
import com.myledger.domain.CompanionType;
import com.myledger.domain.Transaction;
import com.myledger.domain.TransactionType;
import com.myledger.repository.CategoryRepository;
import com.myledger.repository.TransactionRepository;
import com.myledger.service.CsvExportService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CsvExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    @DisplayName("Exports filtered transactions as UTF-8 with BOM CSV preserving Korean memos and escaping quotes/commas")
    void exportsUtf8BomCsvWithProperHeadersAndEscaping() throws Exception {
        Category food = categoryRepository.findByName("Food").orElseThrow();
        Category subscription = categoryRepository.findByName("Subscription").orElseThrow();

        transactionRepository.save(new Transaction(
                TransactionType.EXPENSE,
                food,
                new BigDecimal("12500"),
                LocalDate.of(2026, 11, 24),
                LocalTime.of(12, 30),
                CompanionType.ALONE,
                false,
                "혼밥 점심, 김치찌개 \"특\""
        ));
        // Outside November filter -> should not appear in November export
        transactionRepository.save(new Transaction(
                TransactionType.EXPENSE,
                subscription,
                new BigDecimal("17000"),
                LocalDate.of(2026, 10, 15),
                null,
                CompanionType.NONE,
                true,
                "October Netflix"
        ));

        MvcResult result = mockMvc.perform(get("/transactions/export")
                        .with(user("testadmin").roles("OWNER"))
                        .param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("myledger-transactions-2026-11.csv")))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        // Verify UTF-8 BOM bytes: EF BB BF
        assertThat(bytes).hasSizeGreaterThan(3);
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);

        String content = new String(bytes, StandardCharsets.UTF_8);
        assertThat(content).startsWith(CsvExportService.UTF8_BOM + "Date,Time,Type,Category,Amount,Companion,Fixed,Memo\r\n");
        assertThat(content).contains("2026-11-24,12:30,Expense,Food,12500,Alone,No,\"혼밥 점심, 김치찌개 \"\"특\"\"\"");
        assertThat(content).doesNotContain("October Netflix");
    }
}
