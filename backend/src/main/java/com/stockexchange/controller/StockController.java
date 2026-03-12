package com.stockexchange.controller;

import com.stockexchange.dto.response.StockResponse;
import com.stockexchange.entity.Stock;
import com.stockexchange.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/all")
    public ResponseEntity<List<StockResponse>> getAllStocks() {
        // Public endpoint — permitted in SecurityConfig.
        // Called by the market dashboard to show all stock prices.
        // Also called by the order form to populate the stock dropdown.
        return ResponseEntity.ok(stockService.getAllActiveStocks());
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<StockResponse> getStockByTicker(
            @PathVariable String ticker) {
        // Authenticated users can view individual stock details.
        // Used when user clicks on a stock card to see high/low/open.
        return ResponseEntity.ok(
                stockService.getStockByTicker(ticker));
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    // Only admin can list new stocks on the exchange.
    public ResponseEntity<Stock> addStock(
            @RequestBody Stock stock) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(stockService.addStock(stock));
    }

    @PutMapping("/{ticker}/delist")
    @PreAuthorize("hasRole('ADMIN')")
    // Only admin can delist a stock.
    public ResponseEntity<Map<String, String>> delistStock(
            @PathVariable String ticker) {
        stockService.delistStock(ticker);
        return ResponseEntity.ok(
                Map.of("message", ticker + " delisted successfully"));
    }

    @PutMapping("/initialize-day")
    @PreAuthorize("hasRole('ADMIN')")
    // Admin calls this at market open to reset daily high/low/open prices.
    public ResponseEntity<Map<String, String>> initializeDayPrices() {
        stockService.initializeDayPrices();
        return ResponseEntity.ok(
                Map.of("message", "Day prices initialized for all stocks"));
    }
}