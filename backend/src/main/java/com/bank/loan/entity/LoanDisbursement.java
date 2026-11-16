package com.bank.loan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("loan_disbursement")
public class LoanDisbursement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicationId;

    private LocalDateTime disbursementTime;

    private BigDecimal amount;

    private String accountNo;

    private String bankName;

    private Long operatorId;

    private LocalDateTime createTime;
}
