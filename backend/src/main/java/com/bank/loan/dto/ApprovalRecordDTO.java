package com.bank.loan.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalRecordDTO {

    private Long id;

    private Long applicationId;

    private Long approverId;

    private String approverName;

    private String approvalLevel;

    private String action;

    private String comment;

    private LocalDateTime approvalTime;
}
