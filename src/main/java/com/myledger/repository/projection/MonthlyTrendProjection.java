package com.myledger.repository.projection;

import com.myledger.domain.TransactionType;
import java.math.BigDecimal;

public interface MonthlyTrendProjection {

    Integer getYear();

    Integer getMonth();

    TransactionType getType();

    BigDecimal getTotalAmount();
}
