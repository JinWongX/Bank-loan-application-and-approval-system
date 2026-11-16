package com.bank.loan.controller;

import com.bank.loan.common.Result;
import com.bank.loan.dto.LoanContractDTO;
import com.bank.loan.service.LoanContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class LoanContractController {

    @Autowired
    private LoanContractService loanContractService;

    @PostMapping("/generate/{applicationId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','APPROVER_LEVEL1','APPROVER_LEVEL2','APPROVER_COMMITTEE')")
    public Result<LoanContractDTO> generateContract(@PathVariable Long applicationId) {
        LoanContractDTO dto = loanContractService.generateContract(applicationId);
        return Result.success(dto);
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','MANAGER','ADMIN','APPROVER_LEVEL1','APPROVER_LEVEL2','APPROVER_COMMITTEE','DISBURSEMENT_OFFICER')")
    public Result<LoanContractDTO> getContract(@PathVariable Long applicationId,
                                                Authentication authentication) {
        Long customerId = isCustomer(authentication) ? getCurrentUserId(authentication) : null;
        LoanContractDTO dto = loanContractService.getContractByApplicationId(applicationId, customerId);
        return Result.success(dto);
    }

    @PostMapping("/sign/{contractId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<LoanContractDTO> signContract(@PathVariable Long contractId,
                                                 Authentication authentication) {
        Long customerId = getCurrentUserId(authentication);
        LoanContractDTO dto = loanContractService.signContract(contractId, customerId);
        return Result.success(dto);
    }

    @GetMapping("/my/list")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<List<LoanContractDTO>> listMyContracts(Authentication authentication) {
        Long customerId = getCurrentUserId(authentication);
        List<LoanContractDTO> list = loanContractService.listMyContracts(customerId);
        return Result.success(list);
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
