package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.service.VNPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/v1/VNpayment")
@Tag(name = "VNPay Payment", description = "Endpoints for VNPay integration")
@RequiredArgsConstructor
public class VNPaymentController {
    private final VNPayService vnPayService;

    @GetMapping("/create")
    public String home(){
        return "index";
    }

    @Operation(summary = "Create a new VNPay payment order and redirect")
    @PostMapping("/submitOrder")
    public String submitOrder(@RequestParam("amount") Long orderTotal,
                              @RequestParam("orderInfo") String orderInfo,
                              HttpServletRequest request){
        String vnpayUrl = vnPayService.createOrder(request, orderTotal, orderInfo);
        return "redirect:" + vnpayUrl;
    }

    @Operation(summary = "order VNPay chua xong ", description = "chua xong ")
    @GetMapping("/vnpay-payment")
    public String GetMapping(HttpServletRequest request, Model model){
        int paymentStatus =vnPayService.orderReturn(request);

        String orderInfo = request.getParameter("vnp_OrderInfo");
        String paymentTime = request.getParameter("vnp_PayDate");
        String transactionId = request.getParameter("vnp_TransactionNo");
        String totalPrice = request.getParameter("vnp_Amount");

        model.addAttribute("orderId", orderInfo);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("paymentTime", paymentTime);
        model.addAttribute("transactionId", transactionId);

        return paymentStatus == 1 ? "ordersuccess" : "orderfail";
    }
}
