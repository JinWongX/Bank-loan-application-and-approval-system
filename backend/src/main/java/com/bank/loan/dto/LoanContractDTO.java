package com.bank.loan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoanContractDTO {

    private Long id;

    private Long applicationId;

    private String applicationNo;

    private String contractNo;

    private String contractContent;

    private String signStatus;

    private LocalDateTime signTime;

    private LocalDateTime createTime;

    private String customerName;

    private String idCard;

    private String productName;

    private BigDecimal amount;

    private Integer termMonths;

    private BigDecimal interestRate;

    private String repaymentMethod;
}
