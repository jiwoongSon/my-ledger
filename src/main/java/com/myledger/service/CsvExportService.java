package com.myledger.service;

import com.myledger.domain.Transaction;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CsvExportService {

    public static final String UTF8_BOM = "\uFEFF";
    public static final String HEADER = "Date,Time,Type,Category,Amount,Companion,Fixed,Memo";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public byte[] exportTransactions(List<Transaction> transactions) {
        StringBuilder sb = new StringBuilder();
        sb.append(UTF8_BOM);
        sb.append(HEADER).append("\r\n");

        for (Transaction tx : transactions) {
            sb.append(escapeCsv(tx.getTransactionDate().toString())).append(',');
            sb.append(escapeCsv(tx.getTransactionTime() != null ? tx.getTransactionTime().format(TIME_FORMATTER) : "")).append(',');
            sb.append(escapeCsv(tx.getType().getLabel())).append(',');
            sb.append(escapeCsv(tx.getCategory().getName())).append(',');
            sb.append(escapeCsv(tx.getAmount().toPlainString())).append(',');
            sb.append(escapeCsv(tx.getCompanionType().getLabel())).append(',');
            sb.append(escapeCsv(tx.isFixed() ? "Yes" : "No")).append(',');
            sb.append(escapeCsv(tx.getMemo() != null ? tx.getMemo() : "")).append("\r\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean needsQuotes = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!needsQuotes) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
