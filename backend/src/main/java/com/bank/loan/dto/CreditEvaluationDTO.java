package com.bank.loan.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreditEvaluationDTO {

    private Long id;

    private Long applicationId;

    private Integer score;

    private String grade;

    private String evaluationSuggestion;

    private LocalDateTime evaluateTime;
}
