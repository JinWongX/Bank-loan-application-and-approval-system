package com.bank.loan.controller;

import com.bank.loan.common.Result;
import com.bank.loan.dto.ApprovalActionRequest;
import com.bank.loan.dto.ApprovalPendingDTO;
import com.bank.loan.dto.LoanApplicationDetailDTO;
import com.bank.loan.service.ApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('APPROVER_LEVEL1','APPROVER_LEVEL2','APPROVER_COMMITTEE')")
    public Result<List<ApprovalPendingDTO>> listPendingApplications(Authentication authentication) {
        Long approverId = getCurrentUserId(authentication);
        String role = getCurrentRole(authentication);
        List<ApprovalPendingDTO> list = approvalService.listPendingApplications(approverId, role);
        return Result.success(list);
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('APPROVER_LEVEL1','APPROVER_LEVEL2','APPROVER_COMMITTEE','ADMIN')")
    public Result<LoanApplicationDetailDTO> getApprovalDetail(@PathVariable Long applicationId,
                                                             Authentication authentication) {
        String role = getCurrentRole(authentication);
        LoanApplicationDetailDTO dto = approvalService.getApprovalDetail(applicationId, role);
        return Result.success(dto);
    }

    @PostMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('APPROVER_LEVEL1','APPROVER_LEVEL2','APPROVER_COMMITTEE')")
    public Result<Void> submitApproval(@PathVariable Long applicationId,
                                       @RequestBody ApprovalActionRequest request,
                                       Authentication authentication) {
        Long approverId = getCurrentUserId(authentication);
        String role = getCurrentRole(authentication);
        approvalService.submitApproval(applicationId, approverId, role, request);
        return Result.success();
    }

    private Long getCurrentUserId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        if (details instanceof Number) {
            return ((Number) details).longValue();
        }
        throw new RuntimeException("无法获取当前用户信息");
    }

    private String getCurrentRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .filter(auth -> auth.getAuthority().startsWith("ROLE_"))
                .map(auth -> auth.getAuthority().substring(5).toLowerCase())
                .findFirst()
                .orElse("");
    }
}
