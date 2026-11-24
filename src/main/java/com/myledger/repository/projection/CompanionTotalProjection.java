package com.myledger.repository.projection;

import com.myledger.domain.CompanionType;
import java.math.BigDecimal;

public interface CompanionTotalProjection {

    CompanionType getCompanionType();

    BigDecimal getTotalAmount();

    Long getTransactionCount();
}
