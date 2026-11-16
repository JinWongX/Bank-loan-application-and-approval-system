package com.bank.loan.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoanApplicationDetailDTO extends LoanApplicationDTO {

    private String customerName;

    private String customerPhone;

    private String idCard;

    private CreditEvaluationDTO creditEvaluation;

    private DueDiligenceDTO dueDiligence;

    private List<ApprovalRecordDTO> approvalRecords;
}
