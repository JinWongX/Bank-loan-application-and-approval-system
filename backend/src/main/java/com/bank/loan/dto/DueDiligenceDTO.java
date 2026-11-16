package com.bank.loan.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DueDiligenceDTO {

    private Long id;

    private Long applicationId;

    private Long managerId;

    private String managerName;

    private String businessSituation;

    private String assetsLiabilities;

    private String repaymentAbility;

    private String conclusion;

    private String attachmentPath;

    private LocalDateTime submitTime;
}
