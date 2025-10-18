package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.OrderRequest;
import com.carbonx.marketcarbon.dto.response.MessageResponse;
import com.carbonx.marketcarbon.dto.response.OrderResponse;
import com.carbonx.marketcarbon.service.OrderService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
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
@PreAuthorize("hasRole('COMPANY')")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Buyer company create a new Order" , description = "Buyer company creates a Pending order based on marketplace listing")
    @PostMapping
    public ResponseEntity<TuongCommonResponse<OrderResponse>> createOrder(
            @Valid  @RequestBody TuongCommonRequest<@Valid OrderRequest> request,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
            ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        OrderResponse order = orderService.createOrder(request.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<OrderResponse> response = new TuongCommonResponse<>(now,trace,rs, order);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "System complete a PENDING Order", description = "System executes the financial transaction for a PENDING order. This moves funds and transfers carbon credits.")
    @PostMapping("/{id}/complete")
    public ResponseEntity<TuongCommonResponse<MessageResponse>> completeOrder(
            @PathVariable("id") Long orderId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        orderService.completeOrder(orderId);

        MessageResponse message = new MessageResponse("Order " + orderId + " completed successfully");
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());

        TuongCommonResponse<MessageResponse> response = new TuongCommonResponse<>(now,trace,rs,message);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Get Order by ID", description = "Retrieves the details of a specific order.")
    @GetMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<OrderResponse>> getOrderById(
            @PathVariable("id") Long orderId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
            ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        OrderResponse responseData = orderService.getOrderById(orderId);
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<OrderResponse> response = new TuongCommonResponse<>(trace, now, rs, responseData);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get User's Order History", description = "Retrieves all orders placed by the current user's company.")
    @GetMapping
    public ResponseEntity<TuongCommonResponse<List<OrderResponse>>> getUserOrders(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<OrderResponse> orders = orderService.getUserOrders();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<OrderResponse>> response = new TuongCommonResponse<>(trace, now, rs, orders);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Cancel a PENDING Order", description = "Cancels an order that has not yet been completed.")
    @DeleteMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<MessageResponse>> cancelOrder(
            @PathVariable("id") Long orderId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        orderService.cancelOrder(orderId);

        MessageResponse message = new MessageResponse("Order " + orderId + " has been cancelled.");
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<MessageResponse> response = new TuongCommonResponse<>(trace, now, rs, message);

        return ResponseEntity.ok(response);
    }
}
