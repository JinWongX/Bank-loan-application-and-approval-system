package com.bank.loan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("loan_application")
public class LoanApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String applicationNo;

    private Long customerId;

    private Long productId;

    private BigDecimal amount;

    private Integer termMonths;

    private String purpose;

    private String repaymentSource;

    private String status;

    private Long managerId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
