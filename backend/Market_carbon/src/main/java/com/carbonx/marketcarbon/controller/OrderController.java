package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.OrderRequest;
import com.carbonx.marketcarbon.dto.response.MessageResponse;
import com.carbonx.marketcarbon.dto.response.CreditTradeResponse;
import com.carbonx.marketcarbon.service.OrderService;
import com.carbonx.marketcarbon.utils.common.ApiRequest;
import com.carbonx.marketcarbon.utils.common.ApiResponse;
import com.carbonx.marketcarbon.utils.common.ResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Buyer company create a new Order" , description = "Buyer company creates a Pending order based on marketplace listing")
    @PostMapping
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<ApiResponse<CreditTradeResponse>> createOrder(
            @Valid  @RequestBody ApiRequest<OrderRequest> request,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
            ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        CreditTradeResponse order = orderService.createOrder(request.getData());

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        ApiResponse<CreditTradeResponse> response = new ApiResponse<>(now,trace,rs, order);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "System complete a PENDING Order", description = "System executes the financial transaction for a PENDING order. This moves funds and transfers carbon credits.")
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<MessageResponse>> completeOrder(
            @PathVariable("id") Long orderId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        orderService.completeOrder(orderId);

        MessageResponse message = new MessageResponse("Order " + orderId + " completed successfully");
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());

        ApiResponse<MessageResponse> response = new ApiResponse<>(now,trace,rs,message);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Get Order by ID", description = "Retrieves the details of a specific order.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CreditTradeResponse>> getOrderById(
            @PathVariable("id") Long orderId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
            ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        CreditTradeResponse responseData = orderService.getOrderById(orderId);
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<CreditTradeResponse> response = new ApiResponse<>(trace, now, rs, responseData);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get User's Order History", description = "Retrieves all orders placed by the current user's company.")
    @GetMapping
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<ApiResponse<List<CreditTradeResponse>>> getUserOrders(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<CreditTradeResponse> orders = orderService.getUserOrders();
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<List<CreditTradeResponse>> response = new ApiResponse<>(trace, now, rs, orders);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Cancel a PENDING Order", description = "Cancels an order that has not yet been completed.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> cancelOrder(
            @PathVariable("id") Long orderId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        orderService.cancelOrder(orderId);

        MessageResponse message = new MessageResponse("Order " + orderId + " has been cancelled.");
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<MessageResponse> response = new ApiResponse<>(trace, now, rs, message);

        return ResponseEntity.ok(response);
    }
}
