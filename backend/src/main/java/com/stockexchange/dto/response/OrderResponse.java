package com.stockexchange.dto.response;

import com.stockexchange.enums.OrderSide;
import com.stockexchange.enums.OrderStatus;
import com.stockexchange.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    // Order's database ID — used by frontend to cancel a specific order.

    private String ticker;
    // Which stock this order is for.

    private String companyName;
    // Full company name for display.

    private OrderSide side;
    // BUY or SELL — frontend shows different color for each.

    private OrderType type;
    // MARKET or LIMIT — shown in order history table.

    private OrderStatus status;
    // PENDING / EXECUTED / CANCELLED / REJECTED.
    // Frontend shows appropriate badge color for each status.

    private Integer quantity;
    // Number of shares in this order.

    private BigDecimal limitPrice;
    // Target price for LIMIT orders. Null for MARKET orders.

    private BigDecimal executedPrice;
    // Actual price the order filled at. Null if not yet executed.

    private BigDecimal totalOrderValue;
    // executedPrice × quantity. Null if not yet executed.

    private boolean isMarginOrder;
    // Whether margin was used for this order.

    private String rejectionReason;
    // If status = REJECTED, explains why — "Insufficient balance",
    // "Market is closed", etc. Null for non-rejected orders.

    private LocalDateTime placedAt;
    // When the user placed the order.

    private LocalDateTime executedAt;
    // When the order was filled. Null if pending or rejected.
}