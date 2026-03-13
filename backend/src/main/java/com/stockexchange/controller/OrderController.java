package com.stockexchange.controller;

import com.stockexchange.dto.request.PlaceOrderRequest;
import com.stockexchange.dto.response.OrderResponse;
import com.stockexchange.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/place")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    // Only authenticated users can place orders.
    // OrderService internally validates:
    // market open, balance, shares available, margin limits.
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);

        // Return 201 CREATED for executed/pending orders.
        // Return 200 OK for rejected orders so frontend
        // can read the rejection reason in the body.
        HttpStatus status = switch (response.getStatus()) {
            case EXECUTED, PENDING -> HttpStatus.CREATED;
            case REJECTED          -> HttpStatus.OK;
            case CANCELLED         -> HttpStatus.OK;
        };

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    // Returns all orders placed by the logged-in user.
    // Used for the order history tab on the dashboard.
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    @GetMapping("/my/pending")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    // Returns only PENDING limit orders for the logged-in user.
    // Used to show active limit orders that can be cancelled.
    public ResponseEntity<List<OrderResponse>> getMyPendingOrders() {
        return ResponseEntity.ok(orderService.getMyPendingOrders());
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    // User cancels their own pending limit order.
    // OrderService validates the order belongs to this user
    // and is still in PENDING status before cancelling.
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }

    @PutMapping("/cancel-all-pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    // Admin cancels all pending orders — called at market close.
    // Also triggered automatically by MarketHoursService
    // but exposed here for manual admin override.
    public ResponseEntity<Map<String, String>> cancelAllPending() {
        orderService.cancelAllPendingOrdersAtMarketClose();
        return ResponseEntity.ok(
                Map.of("message",
                        "All pending orders cancelled successfully"));
    }
}