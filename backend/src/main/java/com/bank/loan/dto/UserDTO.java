package com.bank.loan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserDTO {

    private Long id;

    private String username;

    private String realName;

    private String role;

    private String phone;

    private String idCard;

    private Integer age;

    private BigDecimal monthlyIncome;

    private String creditStatus;

    private BigDecimal monthlyDebt;

    private Integer status;

    private LocalDateTime createTime;
}
