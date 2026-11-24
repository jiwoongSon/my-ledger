package com.myledger.repository.projection;

import java.math.BigDecimal;

public interface FixedSplitProjection {

    Boolean getFixed();

    BigDecimal getTotalAmount();

    Long getTransactionCount();
}
