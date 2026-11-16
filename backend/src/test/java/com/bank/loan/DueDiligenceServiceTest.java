package com.bank.loan;

import com.bank.loan.dto.DueDiligenceDTO;
import com.bank.loan.entity.CreditEvaluation;
import com.bank.loan.entity.DueDiligence;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.SysUser;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.mapper.CreditEvaluationMapper;
import com.bank.loan.mapper.DueDiligenceMapper;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.SysUserMapper;
import com.bank.loan.service.LoanProductService;
import com.bank.loan.service.impl.DueDiligenceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DueDiligenceServiceTest {

    @Mock
    private LoanApplicationMapper loanApplicationMapper;

    @Mock
    private DueDiligenceMapper dueDiligenceMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private CreditEvaluationMapper creditEvaluationMapper;

    @Mock
    private LoanProductService loanProductService;

    @InjectMocks
    private DueDiligenceServiceImpl dueDiligenceService;

    private LoanApplication buildApplication(Long id, String status) {
        LoanApplication application = new LoanApplication();
        application.setId(id);
        application.setCustomerId(1L);
        application.setAmount(new java.math.BigDecimal("300000"));
        application.setStatus(status);
        return application;
    }

    private DueDiligenceDTO buildReport(String conclusion) {
        DueDiligenceDTO dto = new DueDiligenceDTO();
        dto.setConclusion(conclusion);
        dto.setBusinessSituation("经营正常");
        dto.setAssetsLiabilities("资产负债率合理");
        dto.setRepaymentAbility("还款能力良好");
        return dto;
    }

    @Test
    void submitReport_supplement_shouldSetMaterialSupplement() {
        LoanApplication application = buildApplication(1L, ApplicationStatus.CREDIT_EVALUATED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(dueDiligenceMapper.selectOne(any())).thenReturn(null);

        dueDiligenceService.submitReport(1L, 10L, buildReport("SUPPLEMENT"));

        assertEquals(ApplicationStatus.MATERIAL_SUPPLEMENT.name(), application.getStatus());
        assertEquals(10L, application.getManagerId());
        verify(dueDiligenceMapper).insert(any());
    }

    @Test
    void submitReport_pass_shouldSetDueDiligenceCompleted() {
        LoanApplication application = buildApplication(1L, ApplicationStatus.CREDIT_EVALUATED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(dueDiligenceMapper.selectOne(any())).thenReturn(null);

        dueDiligenceService.submitReport(1L, 10L, buildReport("PASS"));

        assertEquals(ApplicationStatus.DUE_DILIGENCE_COMPLETED.name(), application.getStatus());
        verify(dueDiligenceMapper).insert(any());
    }

    @Test
    void listPendingApplications_shouldIncludeCreditEvaluated() {
        LoanApplication application = buildApplication(1L, ApplicationStatus.CREDIT_EVALUATED.name());
        when(loanApplicationMapper.selectList(any())).thenReturn(List.of(application));
        when(sysUserMapper.selectById(1L)).thenReturn(new SysUser());
        when(creditEvaluationMapper.selectOne(any())).thenReturn(new CreditEvaluation());

        List<com.bank.loan.dto.LoanApplicationDTO> list = dueDiligenceService.listPendingApplications(10L);

        assertEquals(1, list.size());
        verify(loanApplicationMapper).selectList(any());
    }

    @Test
    void listPendingApplications_shouldFilterManagerIdNullOrMatch() {
        LoanApplication application = buildApplication(1L, ApplicationStatus.CREDIT_EVALUATED.name());
        application.setManagerId(null);
        when(loanApplicationMapper.selectList(any())).thenReturn(List.of(application));
        when(sysUserMapper.selectById(1L)).thenReturn(new SysUser());
        when(creditEvaluationMapper.selectOne(any())).thenReturn(new CreditEvaluation());

        List<com.bank.loan.dto.LoanApplicationDTO> list = dueDiligenceService.listPendingApplications(10L);

        assertEquals(1, list.size());
    }

    @Test
    void submitReport_applicationNotFound_shouldThrow() {
        when(loanApplicationMapper.selectById(99L)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dueDiligenceService.submitReport(99L, 10L, buildReport("PASS")));
        assertEquals("申请不存在", ex.getMessage());
    }

    @Test
    void submitReport_statusNotAllowed_shouldThrow() {
        LoanApplication application = buildApplication(1L, ApplicationStatus.SUBMITTED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dueDiligenceService.submitReport(1L, 10L, buildReport("PASS")));
        assertEquals("当前申请状态不允许提交尽调报告", ex.getMessage());
    }

    @Test
    void submitReport_emptyConclusion_shouldThrow() {
        LoanApplication application = buildApplication(1L, ApplicationStatus.CREDIT_EVALUATED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        DueDiligenceDTO dto = buildReport("");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dueDiligenceService.submitReport(1L, 10L, dto));
        assertEquals("尽调结论不能为空", ex.getMessage());
    }

    @Test
    void submitReport_invalidConclusion_shouldThrow() {
        LoanApplication application = buildApplication(1L, ApplicationStatus.CREDIT_EVALUATED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dueDiligenceService.submitReport(1L, 10L, buildReport("REJECT")));
        assertEquals("尽调结论无效", ex.getMessage());
    }

    @Test
    void submitReport_suggestReject_shouldSetDueDiligenceCompleted() {
        LoanApplication application = buildApplication(1L, ApplicationStatus.CREDIT_EVALUATED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(dueDiligenceMapper.selectOne(any())).thenReturn(null);

        dueDiligenceService.submitReport(1L, 10L, buildReport("SUGGEST_REJECT"));

        assertEquals(ApplicationStatus.DUE_DILIGENCE_COMPLETED.name(), application.getStatus());
        assertEquals(10L, application.getManagerId());
        verify(dueDiligenceMapper).insert(any());
    }

    @Test
    void submitReport_updateExistingReport() {
        LoanApplication application = buildApplication(1L, ApplicationStatus.CREDIT_EVALUATED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        DueDiligence existing = new DueDiligence();
        existing.setId(10L);
        when(dueDiligenceMapper.selectOne(any())).thenReturn(existing);

        dueDiligenceService.submitReport(1L, 10L, buildReport("PASS"));

        assertEquals(ApplicationStatus.DUE_DILIGENCE_COMPLETED.name(), application.getStatus());
        verify(dueDiligenceMapper).updateById(any());
    }

    @Test
    void getReport_empty_shouldReturnNull() {
        when(dueDiligenceMapper.selectOne(any())).thenReturn(null);
        assertNull(dueDiligenceService.getReport(1L));
    }

    @Test
    void getReport_nonEmpty_shouldReturnDto() {
        DueDiligence dueDiligence = new DueDiligence();
        dueDiligence.setId(1L);
        dueDiligence.setApplicationId(1L);
        dueDiligence.setConclusion("PASS");
        dueDiligence.setManagerId(10L);
        when(dueDiligenceMapper.selectOne(any())).thenReturn(dueDiligence);
        when(sysUserMapper.selectById(10L)).thenReturn(new SysUser());

        DueDiligenceDTO dto = dueDiligenceService.getReport(1L);
        assertNotNull(dto);
        assertEquals("PASS", dto.getConclusion());
    }
}
