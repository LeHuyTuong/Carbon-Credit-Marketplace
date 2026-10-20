package com.carbonx.marketcarbon.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6,19}$")
    private String accountNumber; // STK

    @NotBlank
    @Size(min = 2, max = 25)
    private String accountHolderName; // tên chủ tk

    @NotBlank
    @Size(min = 1, max = 5)
    private String bankCode; // VCB/TCB/ACB

    @NotBlank
    private String customerName;

    @OneToOne
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)// WRITE_ONLY = chỉ nhận từ client, không trả về cho client.
    private User user;

    /**
     * Tài khoản đã bị người dùng gỡ nhưng vẫn giữ row: các đơn rút tiền đã xử lý trỏ
     * tới tài khoản này, xoá hẳn row sẽ mất thông tin nơi nhận tiền và bị FK chặn.
     */
    @Column(name = "archived", nullable = false)
    private boolean archived = false;
}
