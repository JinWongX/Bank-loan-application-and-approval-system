package com.bank.loan.dto;

import lombok.Data;

@Data
public class ApprovalActionRequest {

    private String action;

    private String comment;
}
