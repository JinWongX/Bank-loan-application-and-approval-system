package com.bank.loan.controller;

import com.bank.loan.common.Result;
import com.bank.loan.dto.DueDiligenceDTO;
import com.bank.loan.dto.LoanApplicationDTO;
import com.bank.loan.service.DueDiligenceService;
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
@RequestMapping("/api/due-diligence")
public class DueDiligenceController {

    @Autowired
    private DueDiligenceService dueDiligenceService;

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MANAGER')")
    public Result<List<LoanApplicationDTO>> listPending(Authentication authentication) {
        Long managerId = getCurrentUserId(authentication);
        List<LoanApplicationDTO> list = dueDiligenceService.listPendingApplications(managerId);
        return Result.success(list);
    }

    @PostMapping("/{applicationId}")
    @PreAuthorize("hasRole('MANAGER')")
    public Result<DueDiligenceDTO> submitReport(@PathVariable Long applicationId,
                                                 @RequestBody DueDiligenceDTO dto,
                                                 Authentication authentication) {
        Long managerId = getCurrentUserId(authentication);
        DueDiligenceDTO result = dueDiligenceService.submitReport(applicationId, managerId, dto);
        return Result.success(result);
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasRole('MANAGER')")
    public Result<DueDiligenceDTO> getReport(@PathVariable Long applicationId) {
        DueDiligenceDTO dto = dueDiligenceService.getReport(applicationId);
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
}
