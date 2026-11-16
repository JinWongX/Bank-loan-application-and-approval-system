package com.bank.loan.controller;

import com.bank.loan.common.Result;
import com.bank.loan.dto.ApplicationMaterialDTO;
import com.bank.loan.dto.LoanApplicationDTO;
import com.bank.loan.dto.LoanApplicationDetailDTO;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.service.LoanApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class LoanApplicationController {

    @Autowired
    private LoanApplicationService loanApplicationService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<LoanApplicationDTO> submitApplication(@RequestBody LoanApplicationDTO dto,
                                                       Authentication authentication) {
        Long customerId = getCurrentUserId(authentication);
        LoanApplicationDTO result = loanApplicationService.submitApplication(customerId, dto);
        return Result.success(result);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<List<LoanApplicationDTO>> listMyApplications(Authentication authentication) {
        Long customerId = getCurrentUserId(authentication);
        List<LoanApplicationDTO> list = loanApplicationService.listMyApplications(customerId);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','MANAGER','APPROVER_LEVEL1','APPROVER_LEVEL2','APPROVER_COMMITTEE','DISBURSEMENT_OFFICER','ADMIN')")
    public Result<LoanApplicationDTO> getApplication(@PathVariable Long id,
                                                      Authentication authentication) {
        Long customerId = isCustomer(authentication) ? getCurrentUserId(authentication) : null;
        LoanApplicationDTO dto = loanApplicationService.getApplicationDetail(id, customerId);
        return Result.success(dto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','APPROVER_LEVEL1','APPROVER_LEVEL2')")
    public Result<List<LoanApplicationDTO>> listApplications(@RequestParam(required = false) String status) {
        List<LoanApplicationDTO> list = loanApplicationService.listApplications(status);
        return Result.success(list);
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('MANAGER')")
    public Result<List<LoanApplicationDTO>> listAssignedApplications(Authentication authentication) {
        Long managerId = getCurrentUserId(authentication);
        List<LoanApplicationDTO> list = loanApplicationService.listAssignedApplications(managerId);
        return Result.success(list);
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','APPROVER_LEVEL1','APPROVER_LEVEL2','APPROVER_COMMITTEE')")
    public Result<LoanApplicationDetailDTO> getApplicationFullDetail(@PathVariable Long id) {
        LoanApplicationDetailDTO dto = loanApplicationService.getApplicationFullDetail(id);
        return Result.success(dto);
    }

    @GetMapping("/approved")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','APPROVER_LEVEL1','APPROVER_LEVEL2','APPROVER_COMMITTEE','DISBURSEMENT_OFFICER')")
    public Result<List<LoanApplicationDTO>> listApprovedApplications() {
        List<LoanApplicationDTO> list = loanApplicationService.listApplications(ApplicationStatus.APPROVED.name());
        return Result.success(list);
    }

    @PostMapping("/{id}/materials")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<Void> addMaterials(@PathVariable Long id,
                                      @RequestBody List<ApplicationMaterialDTO> materials,
                                      Authentication authentication) {
        if (CollectionUtils.isEmpty(materials)) {
            return Result.error("材料不能为空");
        }
        Long customerId = getCurrentUserId(authentication);
        loanApplicationService.addMaterials(id, customerId, materials);
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

    private boolean isCustomer(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_CUSTOMER".equals(auth.getAuthority()));
    }
}
