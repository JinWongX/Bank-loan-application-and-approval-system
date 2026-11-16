package com.bank.loan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("loan_contract")
public class LoanContract {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicationId;

    private String contractNo;

    private String contractContent;

    private String signStatus;

    private LocalDateTime signTime;

    private LocalDateTime createTime;
}
