package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.PaymentDetailsRequest;
import com.carbonx.marketcarbon.model.PaymentDetails;

public interface PaymentDetailsService {
    PaymentDetails addPaymentDetails(PaymentDetailsRequest request);

    PaymentDetails updatePaymentDetails(PaymentDetailsRequest request);

    void deletePaymentDetails(PaymentDetailsRequest request);

    PaymentDetails getUserPaymentDetails();
}
