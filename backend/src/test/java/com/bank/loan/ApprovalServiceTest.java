package com.bank.loan;

import com.bank.loan.dto.ApprovalActionRequest;
import com.bank.loan.dto.ApprovalPendingDTO;
import com.bank.loan.dto.LoanApplicationDetailDTO;
import com.bank.loan.entity.ApprovalRecord;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.SysUser;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.enums.ApprovalAction;
import com.bank.loan.mapper.ApprovalRecordMapper;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.SysUserMapper;
import com.bank.loan.service.LoanApplicationService;
import com.bank.loan.service.LoanContractService;
import com.bank.loan.service.impl.ApprovalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalServiceTest {

    @Mock
    private LoanApplicationMapper loanApplicationMapper;

    @Mock
    private ApprovalRecordMapper approvalRecordMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private LoanApplicationService loanApplicationService;

    @Mock
    private LoanContractService loanContractService;

    @InjectMocks
    private ApprovalServiceImpl approvalService;

    private List<ApprovalRecord> records;

    @BeforeEach
    void setUp() {
        records = new ArrayList<>();
        when(approvalRecordMapper.selectList(any())).thenAnswer(invocation -> new ArrayList<>(records));
        when(approvalRecordMapper.insert(any(ApprovalRecord.class))).thenAnswer(invocation -> {
            ApprovalRecord record = invocation.getArgument(0);
            record.setId((long) (records.size() + 1));
            records.add(record);
            return 1;
        });
    }

    private LoanApplication buildApplication(Long id, BigDecimal amount, String status) {
        LoanApplication application = new LoanApplication();
        application.setId(id);
        application.setApplicationNo("LA202609010001");
        application.setCustomerId(1L);
        application.setAmount(amount);
        application.setTermMonths(12);
        application.setStatus(status);
        return application;
    }

    private LoanApplicationDetailDTO buildDetail() {
        LoanApplicationDetailDTO dto = new LoanApplicationDetailDTO();
        dto.setId(1L);
        dto.setCreditGrade("B");
        return dto;
    }

    @Test
    void amountUnder500k_level1Approve_shouldBeApprovedAndGenerateContract() {
        LoanApplication application = buildApplication(1L, new BigDecimal("400000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(loanApplicationService.getApplicationDetail(1L, null)).thenReturn(buildDetail());

        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setAction(ApprovalAction.APPROVE.name());
        request.setComment("同意");

        approvalService.submitApproval(1L, 10L, "approver_level1", request);

        assertEquals(ApplicationStatus.APPROVED.name(), application.getStatus());
        verify(loanContractService).generateContract(1L);
        assertEquals(1, records.size());
        assertEquals("LEVEL1", records.get(0).getApprovalLevel());
    }

    @Test
    void amountBetween500kAnd1m_twoLevelApprove_shouldBeApproved() {
        LoanApplication application = buildApplication(1L, new BigDecimal("700000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(loanApplicationService.getApplicationDetail(1L, null)).thenReturn(buildDetail());

        ApprovalActionRequest approveRequest = new ApprovalActionRequest();
        approveRequest.setAction(ApprovalAction.APPROVE.name());
        approveRequest.setComment("通过");

        approvalService.submitApproval(1L, 10L, "approver_level1", approveRequest);
        assertEquals(ApplicationStatus.PENDING_APPROVAL.name(), application.getStatus());

        approvalService.submitApproval(1L, 11L, "approver_level2", approveRequest);
        assertEquals(ApplicationStatus.APPROVED.name(), application.getStatus());
        verify(loanContractService).generateContract(1L);
        assertEquals(2, records.size());
        assertEquals("LEVEL2", records.get(1).getApprovalLevel());
    }

    @Test
    void amountAbove1m_threeLevelApprove_shouldBeApproved() {
        LoanApplication application = buildApplication(1L, new BigDecimal("1500000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(loanApplicationService.getApplicationDetail(1L, null)).thenReturn(buildDetail());

        ApprovalActionRequest approveRequest = new ApprovalActionRequest();
        approveRequest.setAction(ApprovalAction.APPROVE.name());

        approvalService.submitApproval(1L, 10L, "approver_level1", approveRequest);
        assertEquals(ApplicationStatus.PENDING_APPROVAL.name(), application.getStatus());

        approvalService.submitApproval(1L, 11L, "approver_level2", approveRequest);
        assertEquals(ApplicationStatus.PENDING_APPROVAL.name(), application.getStatus());

        approvalService.submitApproval(1L, 12L, "approver_committee", approveRequest);
        assertEquals(ApplicationStatus.APPROVED.name(), application.getStatus());
        verify(loanContractService).generateContract(1L);
        assertEquals(3, records.size());
        assertEquals("COMMITTEE", records.get(2).getApprovalLevel());
    }

    @Test
    void reject_shouldSetRejected() {
        LoanApplication application = buildApplication(1L, new BigDecimal("300000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(loanApplicationService.getApplicationDetail(1L, null)).thenReturn(buildDetail());

        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setAction(ApprovalAction.REJECT.name());
        request.setComment("资质不符");

        approvalService.submitApproval(1L, 10L, "approver_level1", request);

        assertEquals(ApplicationStatus.REJECTED.name(), application.getStatus());
        assertEquals(1, records.size());
        assertEquals(ApprovalAction.REJECT.name(), records.get(0).getAction());
    }

    @Test
    void supplement_shouldReturnToMaterialSupplement() {
        LoanApplication application = buildApplication(1L, new BigDecimal("300000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(loanApplicationService.getApplicationDetail(1L, null)).thenReturn(buildDetail());

        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setAction(ApprovalAction.SUPPLEMENT.name());
        request.setComment("补充收入证明");

        approvalService.submitApproval(1L, 10L, "approver_level1", request);

        assertEquals(ApplicationStatus.MATERIAL_SUPPLEMENT.name(), application.getStatus());
        assertEquals(1, records.size());
        assertEquals(ApprovalAction.SUPPLEMENT.name(), records.get(0).getAction());
    }

    @Test
    void wrongRole_shouldThrow() {
        LoanApplication application = buildApplication(1L, new BigDecimal("300000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);

        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setAction(ApprovalAction.APPROVE.name());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> approvalService.submitApproval(1L, 10L, "manager", request));
        assertEquals("当前角色无权审批", ex.getMessage());
    }

    @Test
    void approveAtWrongLevel_shouldThrow() {
        LoanApplication application = buildApplication(1L, new BigDecimal("700000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(loanApplicationService.getApplicationDetail(1L, null)).thenReturn(buildDetail());

        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setAction(ApprovalAction.APPROVE.name());

        approvalService.submitApproval(1L, 10L, "approver_level1", request);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> approvalService.submitApproval(1L, 12L, "approver_committee", request));
        assertEquals("当前不是您的审批层级", ex.getMessage());
    }

    @Test
    void alreadyApproved_shouldThrow() {
        LoanApplication application = buildApplication(1L, new BigDecimal("300000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(loanApplicationService.getApplicationDetail(1L, null)).thenReturn(buildDetail());

        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setAction(ApprovalAction.APPROVE.name());

        approvalService.submitApproval(1L, 10L, "approver_level1", request);

        application.setStatus(ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> approvalService.submitApproval(1L, 11L, "approver_level1", request));
        assertEquals("当前层级已完成审批", ex.getMessage());
    }

    @Test
    void listPendingApplications_level1OnlySeeLevel1() {
        LoanApplication app = buildApplication(1L, new BigDecimal("300000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        List<LoanApplication> list = new ArrayList<>();
        list.add(app);
        when(loanApplicationMapper.selectList(any())).thenReturn(list);
        when(loanApplicationService.getApplicationDetail(1L, null)).thenReturn(buildDetail());
        when(sysUserMapper.selectById(1L)).thenReturn(new SysUser());

        List<ApprovalPendingDTO> pending = approvalService.listPendingApplications(10L, "approver_level1");

        assertEquals(1, pending.size());
        assertEquals(1, pending.get(0).getCurrentLevel());
    }

    @Test
    void level2Supplement_thenReApproveBothLevels_shouldBeApproved() {
        LoanApplication application = buildApplication(1L, new BigDecimal("700000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(loanApplicationService.getApplicationDetail(1L, null)).thenReturn(buildDetail());

        ApprovalActionRequest approveRequest = new ApprovalActionRequest();
        approveRequest.setAction(ApprovalAction.APPROVE.name());

        // 一级审批通过
        approvalService.submitApproval(1L, 10L, "approver_level1", approveRequest);
        assertEquals(ApplicationStatus.PENDING_APPROVAL.name(), application.getStatus());

        // 二级审批退回补充
        ApprovalActionRequest supplementRequest = new ApprovalActionRequest();
        supplementRequest.setAction(ApprovalAction.SUPPLEMENT.name());
        supplementRequest.setComment("补充经营流水");
        approvalService.submitApproval(1L, 11L, "approver_level2", supplementRequest);
        assertEquals(ApplicationStatus.MATERIAL_SUPPLEMENT.name(), application.getStatus());

        // 模拟客户补充材料后重新信用评估，状态回到 CREDIT_EVALUATED，再经尽调后到 DUE_DILIGENCE_COMPLETED
        application.setStatus(ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        approvalService.submitApproval(1L, 10L, "approver_level1", approveRequest);
        assertEquals(ApplicationStatus.PENDING_APPROVAL.name(), application.getStatus());

        // 二级重新审批通过
        approvalService.submitApproval(1L, 11L, "approver_level2", approveRequest);
        assertEquals(ApplicationStatus.APPROVED.name(), application.getStatus());
        verify(loanContractService).generateContract(1L);
    }

    @Test
    void submitApproval_illegalAction_shouldThrow() {
        LoanApplication application = buildApplication(1L, new BigDecimal("300000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);

        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setAction("INVALID");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> approvalService.submitApproval(1L, 10L, "approver_level1", request));
        assertEquals("审批动作无效", ex.getMessage());
    }

    @Test
    void submitApproval_emptyAction_shouldThrow() {
        LoanApplication application = buildApplication(1L, new BigDecimal("300000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);

        ApprovalActionRequest request = new ApprovalActionRequest();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> approvalService.submitApproval(1L, 10L, "approver_level1", request));
        assertEquals("审批动作不能为空", ex.getMessage());
    }

    @Test
    void submitApproval_statusNotAllowed_shouldThrow() {
        LoanApplication application = buildApplication(1L, new BigDecimal("300000"), ApplicationStatus.SUBMITTED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);

        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setAction(ApprovalAction.APPROVE.name());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> approvalService.submitApproval(1L, 10L, "approver_level1", request));
        assertEquals("当前申请状态不允许审批", ex.getMessage());
    }

    @Test
    void getApprovalDetail_roleNull_shouldThrow() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> approvalService.getApprovalDetail(1L, null));
    }

    @Test
    void getApprovalDetail_admin_shouldReturnFullDetail() {
        LoanApplicationDetailDTO detail = buildDetail();
        when(loanApplicationService.getApplicationFullDetail(1L)).thenReturn(detail);

        LoanApplicationDetailDTO result = approvalService.getApprovalDetail(1L, "admin");
        assertEquals(detail, result);
    }

    @Test
    void getApprovalDetail_level1AmountTooHigh_shouldThrow() {
        LoanApplication application = buildApplication(1L, new BigDecimal("600000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> approvalService.getApprovalDetail(1L, "approver_level1"));
    }

    @Test
    void getApprovalDetail_level2InRange_shouldReturn() {
        LoanApplication application = buildApplication(1L, new BigDecimal("700000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        LoanApplicationDetailDTO detail = buildDetail();
        when(loanApplicationService.getApplicationFullDetail(1L)).thenReturn(detail);

        LoanApplicationDetailDTO result = approvalService.getApprovalDetail(1L, "approver_level2");
        assertNotNull(result);
    }

    @Test
    void getApprovalDetail_committeeAmountTooLow_shouldThrow() {
        LoanApplication application = buildApplication(1L, new BigDecimal("500000"), ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> approvalService.getApprovalDetail(1L, "approver_committee"));
    }

    @Test
    void listPendingApplications_level2OnlySeeAbove500k() {
        LoanApplication app1 = buildApplication(1L, new BigDecimal("300000"), ApplicationStatus.PENDING_APPROVAL.name());
        LoanApplication app2 = buildApplication(2L, new BigDecimal("700000"), ApplicationStatus.PENDING_APPROVAL.name());
        ApprovalRecord record = new ApprovalRecord();
        record.setApplicationId(1L);
        record.setApprovalLevel("LEVEL1");
        record.setAction(ApprovalAction.APPROVE.name());
        record.setApprovalTime(java.time.LocalDateTime.now());
        records.add(record);
        when(loanApplicationMapper.selectList(any())).thenReturn(List.of(app1, app2));
        when(loanApplicationService.getApplicationDetail(any(), eq(null))).thenReturn(buildDetail());
        when(sysUserMapper.selectById(1L)).thenReturn(new SysUser());

        List<ApprovalPendingDTO> pending = approvalService.listPendingApplications(10L, "approver_level2");

        assertEquals(1, pending.size());
        assertEquals(2L, pending.get(0).getId());
    }

    @Test
    void listPendingApplications_committeeOnlySeeAbove1m() {
        LoanApplication app1 = buildApplication(1L, new BigDecimal("700000"), ApplicationStatus.PENDING_APPROVAL.name());
        LoanApplication app2 = buildApplication(2L, new BigDecimal("1500000"), ApplicationStatus.PENDING_APPROVAL.name());
        ApprovalRecord record1 = new ApprovalRecord();
        record1.setApplicationId(1L);
        record1.setApprovalLevel("LEVEL1");
        record1.setAction(ApprovalAction.APPROVE.name());
        record1.setApprovalTime(java.time.LocalDateTime.now());
        ApprovalRecord record2 = new ApprovalRecord();
        record2.setApplicationId(1L);
        record2.setApprovalLevel("LEVEL2");
        record2.setAction(ApprovalAction.APPROVE.name());
        record2.setApprovalTime(java.time.LocalDateTime.now());
        records.add(record1);
        records.add(record2);
        when(loanApplicationMapper.selectList(any())).thenReturn(List.of(app1, app2));
        when(loanApplicationService.getApplicationDetail(any(), eq(null))).thenReturn(buildDetail());
        when(sysUserMapper.selectById(1L)).thenReturn(new SysUser());

        List<ApprovalPendingDTO> pending = approvalService.listPendingApplications(10L, "approver_committee");

        assertEquals(1, pending.size());
        assertEquals(2L, pending.get(0).getId());
    }

    @Test
    void listPendingApplications_unknownRole_shouldReturnEmpty() {
        List<ApprovalPendingDTO> pending = approvalService.listPendingApplications(10L, "manager");
        assertTrue(pending.isEmpty());
    }
}
