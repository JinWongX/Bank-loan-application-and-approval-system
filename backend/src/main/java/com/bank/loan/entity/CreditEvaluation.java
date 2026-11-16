package com.bank.loan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("credit_evaluation")
public class CreditEvaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicationId;

    private Integer score;

    private String grade;

    private String evaluationSuggestion;

    private LocalDateTime evaluateTime;
}
