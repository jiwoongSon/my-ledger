package com.myledger.repository.projection;

import com.myledger.domain.TransactionType;
import java.math.BigDecimal;

public interface TypeTotalProjection {

    TransactionType getType();

    BigDecimal getTotalAmount();
}
