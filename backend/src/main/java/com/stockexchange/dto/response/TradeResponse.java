package com.stockexchange.dto.response;

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
public class TradeResponse {

    private Long id;
    // Trade's database ID.

    private String ticker;
    // Which stock was traded.

    private String companyName;
    // Full company name for display in trade history.

    private String buyerUsername;
    // Who bought — shown in admin trade monitor and trade detail view.

    private String sellerUsername;
    // Who sold — shown in admin trade monitor.

    private Integer quantity;
    // Number of shares that changed hands.

    private BigDecimal executedPrice;
    // Price per share at which the trade settled.

    private BigDecimal totalTradeValue;
    // executedPrice × quantity — total money transferred.

    private String side;
    // "BUY" or "SELL" from the perspective of the requesting user.
    // When user views their own trade history, they see their side —
    // not both buyer and seller perspectives simultaneously.

    private LocalDateTime executedAt;
    // Exact timestamp of the trade.
    // Shown in trade history timeline.
}