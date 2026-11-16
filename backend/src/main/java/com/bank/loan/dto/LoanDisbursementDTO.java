package com.bank.loan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoanDisbursementDTO {

    private Long id;

    private Long applicationId;

    private LocalDateTime disbursementTime;

    private BigDecimal amount;

    private String accountNo;

    private String bankName;

    private Long operatorId;

    private LocalDateTime createTime;
}
