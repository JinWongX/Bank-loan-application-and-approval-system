package com.bank.loan.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateUserRequest {

    private Long id;

    private String realName;

    private String role;

    private String phone;

    private String idCard;

    private Integer age;

    private BigDecimal monthlyIncome;

    private String creditStatus;

    private BigDecimal monthlyDebt;

    private Integer status;
}
