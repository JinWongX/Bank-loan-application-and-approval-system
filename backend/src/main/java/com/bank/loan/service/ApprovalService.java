package com.bank.loan.service;

import com.bank.loan.dto.ApprovalActionRequest;
import com.bank.loan.dto.ApprovalPendingDTO;
import com.bank.loan.dto.LoanApplicationDetailDTO;

import java.util.List;

public interface ApprovalService {

    List<ApprovalPendingDTO> listPendingApplications(Long approverId, String role);

    LoanApplicationDetailDTO getApprovalDetail(Long applicationId, String role);

    void submitApproval(Long applicationId, Long approverId, String role, ApprovalActionRequest request);
}
