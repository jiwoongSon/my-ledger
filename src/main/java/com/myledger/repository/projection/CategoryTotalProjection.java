package com.myledger.repository.projection;

import java.math.BigDecimal;

public interface CategoryTotalProjection {

    Long getCategoryId();

    String getCategoryName();

    BigDecimal getTotalAmount();

    Long getTransactionCount();
}
