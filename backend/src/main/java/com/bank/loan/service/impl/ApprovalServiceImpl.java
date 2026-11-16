package com.bank.loan.service.impl;

import com.bank.loan.dto.ApprovalActionRequest;
import com.bank.loan.dto.ApprovalPendingDTO;
import com.bank.loan.dto.LoanApplicationDetailDTO;
import com.bank.loan.entity.ApprovalRecord;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.SysUser;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.enums.ApprovalAction;
import com.bank.loan.enums.ApprovalLevel;
import com.bank.loan.mapper.ApprovalRecordMapper;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.SysUserMapper;
import com.bank.loan.service.ApprovalService;
import com.bank.loan.service.LoanApplicationService;
import com.bank.loan.service.LoanContractService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApprovalServiceImpl implements ApprovalService {

    private static final BigDecimal AMOUNT_500K = new BigDecimal("500000");
    private static final BigDecimal AMOUNT_1M = new BigDecimal("1000000");

    @Autowired
    private LoanApplicationMapper loanApplicationMapper;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Autowired
    private LoanContractService loanContractService;

    @Override
    public List<ApprovalPendingDTO> listPendingApplications(Long approverId, String role) {
        ApprovalLevel approvalLevel = ApprovalLevel.fromRole(role);
        if (approvalLevel == null) {
            return Collections.emptyList();
        }

        QueryWrapper<LoanApplication> wrapper = new QueryWrapper<>();
        wrapper.in("status", Arrays.asList(
                        ApplicationStatus.DUE_DILIGENCE_COMPLETED.name(),
                        ApplicationStatus.PENDING_APPROVAL.name()))
                .orderByDesc("create_time");
        List<LoanApplication> applications = loanApplicationMapper.selectList(wrapper);

        return applications.stream()
                .map(app -> {
                    List<ApprovalRecord> records = loadApprovalRecords(app.getId());
                    int maxLevel = determineRequiredMaxLevel(app.getAmount());
                    int currentLevel = determineCurrentLevel(app, records, maxLevel);
                    ApprovalPendingDTO dto = new ApprovalPendingDTO();
                    BeanUtils.copyProperties(app, dto);
                    dto.setProduct(loanApplicationService.getApplicationDetail(app.getId(), null).getProduct());
                    dto.setCustomerName(loadCustomerName(app.getCustomerId()));
                    dto.setCreditGrade(loadCreditGrade(app.getId()));
                    dto.setCurrentLevel(currentLevel);
                    dto.setCurrentLevelLabel(buildLevelLabel(currentLevel));
                    return dto;
                })
                .filter(dto -> isVisibleToApprover(dto, approvalLevel))
                .collect(Collectors.toList());
    }

    @Override
    public LoanApplicationDetailDTO getApprovalDetail(Long applicationId, String role) {
        if (role == null) {
            throw new AccessDeniedException("无权查看该审批");
        }
        if ("admin".equalsIgnoreCase(role)) {
            return loanApplicationService.getApplicationFullDetail(applicationId);
        }
        ApprovalLevel approvalLevel = ApprovalLevel.fromRole(role);
        if (approvalLevel == null) {
            throw new AccessDeniedException("当前角色无权查看审批");
        }
        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        List<ApprovalRecord> records = loadApprovalRecords(applicationId);
        int maxLevel = determineRequiredMaxLevel(application.getAmount());
        int currentLevel = determineCurrentLevel(application, records, maxLevel);
        ApprovalPendingDTO tempDto = new ApprovalPendingDTO();
        tempDto.setCurrentLevel(currentLevel);
        tempDto.setAmount(application.getAmount());
        if (!isVisibleToApprover(tempDto, approvalLevel)) {
            throw new AccessDeniedException("该申请不在您的审批权限范围内");
        }
        return loanApplicationService.getApplicationFullDetail(applicationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long applicationId, Long approverId, String role, ApprovalActionRequest request) {
        ApprovalLevel approvalLevel = ApprovalLevel.fromRole(role);
        if (approvalLevel == null) {
            throw new RuntimeException("当前角色无权审批");
        }

        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }

        String status = application.getStatus();
        if (!ApplicationStatus.DUE_DILIGENCE_COMPLETED.name().equals(status)
                && !ApplicationStatus.PENDING_APPROVAL.name().equals(status)) {
            throw new RuntimeException("当前申请状态不允许审批");
        }

        if (!StringUtils.hasText(request.getAction())) {
            throw new RuntimeException("审批动作不能为空");
        }
        ApprovalAction action;
        try {
            action = ApprovalAction.valueOf(request.getAction());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("审批动作无效");
        }

        List<ApprovalRecord> records = loadApprovalRecords(applicationId);
        int maxLevel = determineRequiredMaxLevel(application.getAmount());
        int currentLevel = determineCurrentLevel(application, records, maxLevel);

        if (approvalLevel.getLevel() != currentLevel) {
            throw new RuntimeException("当前不是您的审批层级");
        }

        if (existsApprovedRecordAtLevel(records, currentLevel)) {
            throw new RuntimeException("当前层级已完成审批");
        }

        LocalDateTime now = LocalDateTime.now();

        switch (action) {
            case APPROVE:
                deleteStaleApprovedRecordsAtLevel(records, applicationId, currentLevel);
                saveApprovalRecord(applicationId, approverId, currentLevel, action.name(), request.getComment(), now);
                if (currentLevel == maxLevel) {
                    application.setStatus(ApplicationStatus.APPROVED.name());
                    application.setUpdateTime(now);
                    loanApplicationMapper.updateById(application);
                    loanContractService.generateContract(applicationId);
                } else {
                    application.setStatus(ApplicationStatus.PENDING_APPROVAL.name());
                    application.setUpdateTime(now);
                    loanApplicationMapper.updateById(application);
                }
                break;
            case REJECT:
                saveApprovalRecord(applicationId, approverId, currentLevel, action.name(), request.getComment(), now);
                application.setStatus(ApplicationStatus.REJECTED.name());
                application.setUpdateTime(now);
                loanApplicationMapper.updateById(application);
                break;
            case SUPPLEMENT:
                saveApprovalRecord(applicationId, approverId, currentLevel, action.name(), request.getComment(), now);
                application.setStatus(ApplicationStatus.MATERIAL_SUPPLEMENT.name());
                application.setUpdateTime(now);
                loanApplicationMapper.updateById(application);
                break;
            default:
                throw new RuntimeException("审批动作无效");
        }
    }

    private int determineRequiredMaxLevel(BigDecimal amount) {
        if (amount == null) {
            throw new RuntimeException("申请金额不能为空");
        }
        if (amount.compareTo(AMOUNT_500K) <= 0) {
            return ApprovalLevel.LEVEL1.getLevel();
        }
        if (amount.compareTo(AMOUNT_1M) <= 0) {
            return ApprovalLevel.LEVEL2.getLevel();
        }
        return ApprovalLevel.COMMITTEE.getLevel();
    }

    private int determineCurrentLevel(LoanApplication application, List<ApprovalRecord> records, int maxLevel) {
        String status = application.getStatus();
        if (ApplicationStatus.DUE_DILIGENCE_COMPLETED.name().equals(status)
                || ApplicationStatus.SUBMITTED.name().equals(status)) {
            return ApprovalLevel.LEVEL1.getLevel();
        }
        if (ApplicationStatus.PENDING_APPROVAL.name().equals(status)) {
            Set<Integer> approvedLevels = records.stream()
                    .filter(r -> ApprovalAction.APPROVE.name().equals(r.getAction()))
                    .map(r -> parseApprovalLevel(r.getApprovalLevel()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (int i = 1; i <= maxLevel; i++) {
                if (!approvedLevels.contains(i)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private boolean isVisibleToApprover(ApprovalPendingDTO dto, ApprovalLevel level) {
        int currentLevel = dto.getCurrentLevel() == null ? 0 : dto.getCurrentLevel();
        if (currentLevel <= 0) {
            return false;
        }
        switch (level) {
            case LEVEL1:
                return currentLevel == 1;
            case LEVEL2:
                return currentLevel == 2 && dto.getAmount() != null
                        && dto.getAmount().compareTo(AMOUNT_500K) > 0;
            case COMMITTEE:
                return currentLevel == 3 && dto.getAmount() != null
                        && dto.getAmount().compareTo(AMOUNT_1M) > 0;
            default:
                return false;
        }
    }

    private boolean existsApprovedRecordAtLevel(List<ApprovalRecord> records, int level) {
        Optional<LocalDateTime> lastSupplementTime = getLastSupplementTime(records);
        return records.stream()
                .filter(r -> lastSupplementTime.isPresent()
                        ? r.getApprovalTime() != null && !r.getApprovalTime().isBefore(lastSupplementTime.get())
                        : r.getApprovalTime() != null)
                .anyMatch(r -> ApprovalAction.APPROVE.name().equals(r.getAction())
                        && r.getApprovalLevel() != null
                        && level == parseApprovalLevel(r.getApprovalLevel()));
    }

    private Optional<LocalDateTime> getLastSupplementTime(List<ApprovalRecord> records) {
        return records.stream()
                .filter(r -> ApprovalAction.SUPPLEMENT.name().equals(r.getAction())
                        && r.getApprovalTime() != null)
                .max(Comparator.comparing(ApprovalRecord::getApprovalTime))
                .map(ApprovalRecord::getApprovalTime);
    }

    private void deleteStaleApprovedRecordsAtLevel(List<ApprovalRecord> records, Long applicationId, int level) {
        Optional<LocalDateTime> lastSupplementTime = getLastSupplementTime(records);
        if (!lastSupplementTime.isPresent()) {
            return;
        }
        List<Long> staleIds = records.stream()
                .filter(r -> ApprovalAction.APPROVE.name().equals(r.getAction())
                        && r.getApprovalLevel() != null
                        && level == parseApprovalLevel(r.getApprovalLevel())
                        && r.getApprovalTime() != null
                        && r.getApprovalTime().isBefore(lastSupplementTime.get())
                        && r.getId() != null)
                .map(ApprovalRecord::getId)
                .collect(Collectors.toList());
        if (!staleIds.isEmpty()) {
            QueryWrapper<ApprovalRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("application_id", applicationId).in("id", staleIds);
            approvalRecordMapper.delete(wrapper);
        }
    }

    private Integer parseApprovalLevel(String approvalLevel) {
        if (!StringUtils.hasText(approvalLevel)) {
            return null;
        }
        try {
            return ApprovalLevel.valueOf(approvalLevel).getLevel();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void saveApprovalRecord(Long applicationId, Long approverId, int level,
                                    String action, String comment, LocalDateTime time) {
        ApprovalRecord record = new ApprovalRecord();
        record.setApplicationId(applicationId);
        record.setApproverId(approverId);
        record.setApprovalLevel(ApprovalLevel.fromLevel(level).name());
        record.setAction(action);
        record.setComment(comment);
        record.setApprovalTime(time);
        approvalRecordMapper.insert(record);
    }

    private List<ApprovalRecord> loadApprovalRecords(Long applicationId) {
        QueryWrapper<ApprovalRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("application_id", applicationId).orderByDesc("approval_time");
        return approvalRecordMapper.selectList(wrapper);
    }

    private String loadCustomerName(Long customerId) {
        SysUser customer = sysUserMapper.selectById(customerId);
        return customer == null ? null : customer.getRealName();
    }

    private String loadCreditGrade(Long applicationId) {
        return loanApplicationService.getApplicationDetail(applicationId, null).getCreditGrade();
    }

    private String buildLevelLabel(int level) {
        ApprovalLevel approvalLevel = ApprovalLevel.fromLevel(level);
        return approvalLevel == null ? "" : approvalLevel.getLabel();
    }
}
