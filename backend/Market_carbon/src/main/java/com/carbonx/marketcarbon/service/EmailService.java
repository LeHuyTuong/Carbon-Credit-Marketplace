package com.carbonx.marketcarbon.service;

import jakarta.mail.MessagingException;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface EmailService {

    void sendEmail(String subject, String content, List<String> toList) throws MessagingException,
            UnsupportedEncodingException;
    String renderCvaDecisionEmail(Map<String, Object> variables);
    String renderAdminDecisionEmail(Map<String, Object> variables);

    void send(String to, String subject, String body) throws MessagingException;

    void sendHtml(String to, String subject, String html) throws MessagingException;
    void sendEmailWithAttachment(String to, String subject, String htmlBody, byte[] file, String filename);


    String renderWithdrawalConfirmationEmail(Map<String, Object> variables);

    String renderWithdrawalFailedEmail(Map<String, Object> variables);
    String renderReportCvaDecisionEmail(Map<String, Object> vars);
    String renderReportAdminDecisionEmail(Map<String, Object> vars);

    String renderPayoutSuccessEmail(Map<String, Object> variables);
    String renderPayoutSummaryEmail(Map<String, Object> variables);

    void sendPayoutSuccessToOwner(String toEmail,
                                  String ownerName,
                                  String companyName,
                                  String periodLabel,
                                  BigDecimal totalEnergyKWh,
                                  BigDecimal totalCredits,
                                  BigDecimal amountUsd,
                                  List<VehiclePayoutRow> perVehicle,
                                  String distributionReference,
                                  Long companyId,
                                  String reportReference,
                                  BigDecimal minPayout);

    void sendDistributionSummaryToCompany(String toEmail,
                                          String companyName,
                                          String periodLabel,
                                          int ownersPaid,
                                          BigDecimal totalEnergy,
                                          BigDecimal totalCredits,
                                          BigDecimal totalPayoutUsd,
                                          boolean scaledByCap,
                                          Long companyId,
                                          String distributionReference);

    record VehiclePayoutRow(String plate,
                            String vehicleNameOrModel,
                            BigDecimal energyKWh,
                            BigDecimal credits,
                            BigDecimal amountUsd) {
    }
}
