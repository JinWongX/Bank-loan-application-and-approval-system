package com.bank.loan.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanProductDTO {

    private Long id;

    private String name;

    private String type;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private BigDecimal interestRate;

    private Integer minTerm;

    private Integer maxTerm;

    private String repaymentMethod;

    private String eligibilityConditions;

    private String status;
}
