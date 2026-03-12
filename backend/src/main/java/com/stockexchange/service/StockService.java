package com.stockexchange.service;

import com.stockexchange.dto.response.StockResponse;
import com.stockexchange.entity.Stock;
import com.stockexchange.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    // ── Public Reads ─────────────────────────────────────────────────────────

    public List<StockResponse> getAllActiveStocks() {
        // Called by StockController for the market dashboard.
        // Returns only active (listed) stocks.
        return stockRepository.findByIsActiveTrue()
                .stream()
                .map(this::toStockResponse)
                .toList();
    }

    public StockResponse getStockByTicker(String ticker) {
        Stock stock = stockRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new RuntimeException(
                        "Stock not found: " + ticker));
        return toStockResponse(stock);
    }

    public Stock getStockEntityByTicker(String ticker) {
        // Returns raw entity — used internally by OrderService and simulator.
        return stockRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new RuntimeException(
                        "Stock not found: " + ticker));
    }

    // ── Admin Controls ───────────────────────────────────────────────────────

    @Transactional
    public Stock addStock(Stock stock) {
        // Admin adds a new stock to the exchange.
        if (stockRepository.existsByTicker(stock.getTicker())) {
            throw new RuntimeException(
                    "Stock already exists: " + stock.getTicker());
        }
        stock.setTicker(stock.getTicker().toUpperCase());
        stock.setActive(true);
        stock.setLastUpdatedAt(LocalDateTime.now());
        Stock saved = stockRepository.save(stock);
        log.info("New stock listed: {}", saved.getTicker());
        return saved;
    }

    @Transactional
    public Stock delistStock(String ticker) {
        // Admin removes a stock from active trading.
        Stock stock = getStockEntityByTicker(ticker);
        stock.setActive(false);
        stock.setLastUpdatedAt(LocalDateTime.now());
        log.info("Stock delisted: {}", ticker);
        return stockRepository.save(stock);
    }

    @Transactional
    public void updateStockPrice(String ticker, BigDecimal newPrice) {
        // Called by StockPriceSimulator on every price tick.
        Stock stock = getStockEntityByTicker(ticker);

        // Update high/low of the day
        if (newPrice.compareTo(stock.getHighPrice()) > 0) {
            stock.setHighPrice(newPrice);
        }
        if (newPrice.compareTo(stock.getLowPrice()) < 0) {
            stock.setLowPrice(newPrice);
        }

        stock.setCurrentPrice(newPrice);
        stock.setLastUpdatedAt(LocalDateTime.now());
        stockRepository.save(stock);
    }

    @Transactional
    public void initializeDayPrices() {
        // Called at market open to set openPrice = currentPrice
        // and reset high/low for the new trading day.
        stockRepository.findByIsActiveTrue().forEach(stock -> {
            stock.setOpenPrice(stock.getCurrentPrice());
            stock.setPreviousClosePrice(stock.getCurrentPrice());
            stock.setHighPrice(stock.getCurrentPrice());
            stock.setLowPrice(stock.getCurrentPrice());
            stock.setLastUpdatedAt(LocalDateTime.now());
            stockRepository.save(stock);
        });
        log.info("Day prices initialized for all active stocks");
    }

    // ── Response Mapper ──────────────────────────────────────────────────────

    public StockResponse toStockResponse(Stock stock) {
        BigDecimal change = stock.getCurrentPrice()
                .subtract(stock.getPreviousClosePrice());
        // priceChangeAmount = currentPrice - previousClosePrice

        BigDecimal changePercent = BigDecimal.ZERO;
        if (stock.getPreviousClosePrice().compareTo(BigDecimal.ZERO) != 0) {
            changePercent = change
                    .divide(stock.getPreviousClosePrice(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        String direction = change.compareTo(BigDecimal.ZERO) >= 0 ? "UP" : "DOWN";

        return StockResponse.builder()
                .id(stock.getId())
                .ticker(stock.getTicker())
                .companyName(stock.getCompanyName())
                .currentPrice(stock.getCurrentPrice())
                .openPrice(stock.getOpenPrice())
                .highPrice(stock.getHighPrice())
                .lowPrice(stock.getLowPrice())
                .previousClosePrice(stock.getPreviousClosePrice())
                .priceChangeAmount(change)
                .priceChangePercent(changePercent)
                .priceDirection(direction)
                .totalSharesAvailable(stock.getTotalSharesAvailable())
                .isActive(stock.isActive())
                .lastUpdatedAt(stock.getLastUpdatedAt())
                .build();
    }
}