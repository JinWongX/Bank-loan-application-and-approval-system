package com.bank.loan.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateUserRequest {

    private String username;

    private String password;

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
