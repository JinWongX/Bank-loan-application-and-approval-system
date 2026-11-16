package com.bank.loan.controller;

import com.bank.loan.common.Result;
import com.bank.loan.dto.CreditEvaluationDTO;
import com.bank.loan.service.CreditEvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credit-evaluations")
public class CreditEvaluationController {

    @Autowired
    private CreditEvaluationService creditEvaluationService;

    @PostMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Result<CreditEvaluationDTO> evaluate(@PathVariable Long applicationId) {
        CreditEvaluationDTO dto = creditEvaluationService.evaluate(applicationId);
        return Result.success(dto);
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','MANAGER','ADMIN','APPROVER_LEVEL1','APPROVER_LEVEL2','APPROVER_COMMITTEE','DISBURSEMENT_OFFICER')")
    public Result<CreditEvaluationDTO> getEvaluation(@PathVariable Long applicationId,
                                                     Authentication authentication) {
        Long customerId = isCustomer(authentication) ? getCurrentUserId(authentication) : null;
        CreditEvaluationDTO dto = creditEvaluationService.getEvaluation(applicationId, customerId);
        return Result.success(dto);
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

    private boolean isCustomer(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_CUSTOMER".equals(auth.getAuthority()));
    }
}
