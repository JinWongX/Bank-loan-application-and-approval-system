package com.bank.loan.controller;

import com.bank.loan.common.Result;
import com.bank.loan.dto.LoanApplicationDTO;
import com.bank.loan.dto.LoanDisbursementDTO;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.service.LoanDisbursementService;
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
@RequestMapping("/api/disbursements")
public class LoanDisbursementController {

    @Autowired
    private LoanDisbursementService loanDisbursementService;

    @Autowired
    private LoanApplicationMapper loanApplicationMapper;

    @GetMapping("/pending")
    @PreAuthorize("hasRole('DISBURSEMENT_OFFICER')")
    public Result<List<LoanApplicationDTO>> listPendingApplications() {
        List<LoanApplicationDTO> list = loanDisbursementService.listPendingApplications();
        return Result.success(list);
    }

    @PostMapping("/{applicationId}")
    @PreAuthorize("hasRole('DISBURSEMENT_OFFICER')")
    public Result<LoanDisbursementDTO> disburse(@PathVariable Long applicationId,
                                                 @RequestBody LoanDisbursementDTO dto,
                                                 Authentication authentication) {
        Long operatorId = getCurrentUserId(authentication);
        LoanDisbursementDTO result = loanDisbursementService.disburse(applicationId, dto, operatorId);
        return Result.success(result);
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','DISBURSEMENT_OFFICER')")
    public Result<LoanDisbursementDTO> getDisbursement(@PathVariable Long applicationId,
                                                        Authentication authentication) {
        if (isCustomer(authentication)) {
            Long customerId = getCurrentUserId(authentication);
            LoanApplication application = loanApplicationMapper.selectById(applicationId);
            if (application == null) {
                return Result.error("申请不存在");
            }
            if (!customerId.equals(application.getCustomerId())) {
                return Result.error("无权查看该放款信息");
            }
        }
        LoanDisbursementDTO dto = loanDisbursementService.getDisbursementByApplicationId(applicationId);
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
