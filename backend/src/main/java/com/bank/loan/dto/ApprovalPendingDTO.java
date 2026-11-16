package com.bank.loan.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalPendingDTO extends LoanApplicationDTO {

    private Integer currentLevel;

    private String currentLevelLabel;
}
