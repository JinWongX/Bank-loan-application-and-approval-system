package com.bank.loan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("repayment_schedule")
public class RepaymentSchedule {

    @TableId(type = IdType.AUTO)
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
