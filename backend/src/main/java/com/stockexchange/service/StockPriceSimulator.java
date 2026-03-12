package com.stockexchange.service;

import com.stockexchange.entity.Stock;
import com.stockexchange.repository.StockRepository;
import com.stockexchange.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceSimulator {
    // Runs on a fixed schedule during market hours.
    // Each tick: updates prices → checks limit orders → pushes to frontend via WebSocket.

    private final StockRepository      stockRepository;
    private final StockService         stockService;
    private final OrderService         orderService;
    private final MarketHoursService   marketHoursService;
    private final SimpMessagingTemplate messagingTemplate;
    // SimpMessagingTemplate sends messages to WebSocket subscribers.
    // Frontend subscribes to "/topic/prices" to receive live updates.

    @Value("${simulator.max.change.percent}")
    private double maxChangePercent;
    // 2.0 from application.properties — max ±2% change per tick.

    private final Random random = new Random();

    // ── Scheduled Price Update ───────────────────────────────────────────────

    @Scheduled(fixedRateString = "${simulator.update.interval}")
    // Runs every 5000ms (5 seconds) as set in application.properties.
    // @EnableScheduling in StockExchangeApplication activates this.
    public void simulatePriceChanges() {
        if (!marketHoursService.isMarketOpen()) {
            return;
            // Do nothing outside market hours — prices freeze.
        }

        List<Stock> activeStocks = stockRepository.findByIsActiveTrue();

        activeStocks.forEach(stock -> {
            BigDecimal newPrice = calculateNewPrice(stock.getCurrentPrice());
            stockService.updateStockPrice(stock.getTicker(), newPrice);

            // After price update, check if any limit orders can now execute.
            orderService.checkAndExecuteLimitOrders(
                    stock.getId(), newPrice);

            // Push updated price to all WebSocket subscribers.
            pushPriceUpdate(stock.getTicker(), newPrice);
        });
    }

    // ── Price Calculation ────────────────────────────────────────────────────

    private BigDecimal calculateNewPrice(BigDecimal currentPrice) {
        // Generates a random price movement within ±maxChangePercent.
        double changePercent = (random.nextDouble()
                * maxChangePercent * 2) - maxChangePercent;
        // random.nextDouble() → 0.0 to 1.0
        // × 2 → 0.0 to 2.0
        // - maxChangePercent → -2.0 to +2.0
        // So price changes by between -2% and +2% each tick.

        BigDecimal changeMultiplier = BigDecimal.ONE
                .add(BigDecimal.valueOf(changePercent / 100));

        BigDecimal newPrice = currentPrice
                .multiply(changeMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        // Floor at 1.00 — stock price can never go negative or zero.
        return newPrice.compareTo(BigDecimal.ONE) < 0
                ? BigDecimal.ONE : newPrice;
    }

    // ── WebSocket Push ───────────────────────────────────────────────────────

    private void pushPriceUpdate(String ticker, BigDecimal newPrice) {
        // Sends price update to "/topic/prices" WebSocket channel.
        // All connected frontend clients subscribed to this topic
        // receive the update instantly without polling.
        messagingTemplate.convertAndSend(
                "/topic/prices",
                new PriceUpdate(ticker, newPrice));
    }

    // ── Inner Record ─────────────────────────────────────────────────────────

    public record PriceUpdate(String ticker, BigDecimal price) {
        // Simple payload sent over WebSocket.
        // Jackson serializes this to: {"ticker":"TCS","price":3512.50}
    }
}