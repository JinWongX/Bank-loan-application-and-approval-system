package com.bank.loan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RepaymentScheduleDTO {

    private Long id;

    private Long applicationId;

    private Integer period;

    private LocalDate dueDate;

    private BigDecimal principal;

    private BigDecimal interest;

    private BigDecimal totalAmount;

    private BigDecimal remainingPrincipal;

    private String status;
}
