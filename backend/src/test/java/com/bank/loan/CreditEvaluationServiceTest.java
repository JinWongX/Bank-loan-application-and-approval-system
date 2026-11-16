package com.bank.loan;

import com.bank.loan.dto.CreditEvaluationDTO;
import com.bank.loan.entity.CreditEvaluation;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.SysUser;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.mapper.CreditEvaluationMapper;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.SysUserMapper;
import com.bank.loan.service.impl.CreditEvaluationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditEvaluationServiceTest {

    @Mock
    private LoanApplicationMapper loanApplicationMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private CreditEvaluationMapper creditEvaluationMapper;

    @InjectMocks
    private CreditEvaluationServiceImpl creditEvaluationService;

    private LoanApplication buildApplication() {
        LoanApplication application = new LoanApplication();
        application.setId(1L);
        application.setCustomerId(1L);
        application.setStatus(ApplicationStatus.SUBMITTED.name());
        return application;
    }

    private SysUser buildUser(int age, BigDecimal income, String creditStatus, BigDecimal debt) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setAge(age);
        user.setMonthlyIncome(income);
        user.setCreditStatus(creditStatus);
        user.setMonthlyDebt(debt);
        return user;
    }

    @Test
    void evaluate_highQualityCustomer_shouldBeGradeA() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(35, new BigDecimal("30000"), "GOOD", new BigDecimal("0")));
        when(creditEvaluationMapper.selectOne(any())).thenReturn(null);

        CreditEvaluationDTO result = creditEvaluationService.evaluate(1L);

        assertEquals(100, result.getScore());
        assertEquals("A", result.getGrade());
        assertEquals("信用优秀，建议放款", result.getEvaluationSuggestion());

        verify(loanApplicationMapper).updateById(any(LoanApplication.class));
        ArgumentCaptor<LoanApplication> appCaptor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(loanApplicationMapper).updateById(appCaptor.capture());
        assertEquals(ApplicationStatus.CREDIT_EVALUATED.name(), appCaptor.getValue().getStatus());
    }

    @Test
    void evaluate_goodCustomer_shouldBeGradeB() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(30, new BigDecimal("12000"), "FAIR", new BigDecimal("3600")));
        when(creditEvaluationMapper.selectOne(any())).thenReturn(null);

        CreditEvaluationDTO result = creditEvaluationService.evaluate(1L);

        assertEquals(70, result.getScore());
        assertEquals("B", result.getGrade());
        assertEquals("信用良好，建议正常审批", result.getEvaluationSuggestion());
    }

    @Test
    void evaluate_poorCustomer_shouldBeGradeD() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(60, new BigDecimal("4000"), "POOR", new BigDecimal("2800")));
        when(creditEvaluationMapper.selectOne(any())).thenReturn(null);

        CreditEvaluationDTO result = creditEvaluationService.evaluate(1L);

        assertEquals(15, result.getScore());
        assertEquals("D", result.getGrade());
        assertTrue(result.getEvaluationSuggestion().contains("拒绝"));
        assertTrue(result.getEvaluationSuggestion().contains("负债率偏高"));
    }

    @Test
    void evaluate_updateExistingEvaluation() {
        CreditEvaluation existing = new CreditEvaluation();
        existing.setId(10L);
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(25, new BigDecimal("10000"), "GOOD", new BigDecimal("0")));
        when(creditEvaluationMapper.selectOne(any())).thenReturn(existing);

        CreditEvaluationDTO result = creditEvaluationService.evaluate(1L);

        assertEquals(10L, result.getId());
        assertTrue(result.getScore() > 0);
        verify(creditEvaluationMapper).updateById(any(CreditEvaluation.class));
    }

    @Test
    void evaluate_incomeBoundary() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(35, new BigDecimal("10000"), "GOOD", new BigDecimal("0")));
        when(creditEvaluationMapper.selectOne(any())).thenReturn(null);

        CreditEvaluationDTO result = creditEvaluationService.evaluate(1L);

        assertEquals(25 + 20 + 25 + 20, result.getScore());
        assertEquals("A", result.getGrade());
    }

    @Test
    void evaluate_applicationNotFound_shouldThrow() {
        when(loanApplicationMapper.selectById(99L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> creditEvaluationService.evaluate(99L));
    }

    @Test
    void evaluate_customerNotFound_shouldThrow() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(sysUserMapper.selectById(1L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> creditEvaluationService.evaluate(1L));
    }

    @Test
    void evaluate_gradeC_shouldSuggestSupplement() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(20, new BigDecimal("8000"), "FAIR", new BigDecimal("3000")));
        when(creditEvaluationMapper.selectOne(any())).thenReturn(null);

        CreditEvaluationDTO result = creditEvaluationService.evaluate(1L);

        assertEquals("C", result.getGrade());
        assertTrue(result.getEvaluationSuggestion().contains("补充材料或担保"));
    }

    @Test
    void evaluate_highDebtRatio_shouldAppendCaution() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(35, new BigDecimal("10000"), "GOOD", new BigDecimal("6000")));
        when(creditEvaluationMapper.selectOne(any())).thenReturn(null);

        CreditEvaluationDTO result = creditEvaluationService.evaluate(1L);

        assertTrue(result.getEvaluationSuggestion().contains("负债率偏高"));
    }

    @Test
    void evaluate_creditStatusNull_shouldUseLowestScore() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(35, new BigDecimal("30000"), null, BigDecimal.ZERO));
        when(creditEvaluationMapper.selectOne(any())).thenReturn(null);

        CreditEvaluationDTO result = creditEvaluationService.evaluate(1L);

        assertEquals(25 + 30 + 5 + 20, result.getScore());
    }

    @Test
    void evaluate_creditStatusAbnormal_shouldUseLowestScore() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(35, new BigDecimal("30000"), "UNKNOWN", BigDecimal.ZERO));
        when(creditEvaluationMapper.selectOne(any())).thenReturn(null);

        CreditEvaluationDTO result = creditEvaluationService.evaluate(1L);

        assertEquals(25 + 30 + 5 + 20, result.getScore());
    }

    @Test
    void evaluate_ageBoundary_25And45_shouldMaxScore() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(25, new BigDecimal("30000"), "GOOD", BigDecimal.ZERO));
        when(creditEvaluationMapper.selectOne(any())).thenReturn(null);

        assertEquals(100, creditEvaluationService.evaluate(1L).getScore());

        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(45, new BigDecimal("30000"), "GOOD", BigDecimal.ZERO));
        assertEquals(100, creditEvaluationService.evaluate(1L).getScore());
    }

    @Test
    void evaluate_ageBoundary_18And55_shouldMiddleScore() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(creditEvaluationMapper.selectOne(any())).thenReturn(null);

        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(18, new BigDecimal("30000"), "GOOD", BigDecimal.ZERO));
        assertEquals(90, creditEvaluationService.evaluate(1L).getScore());

        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(55, new BigDecimal("30000"), "GOOD", BigDecimal.ZERO));
        assertEquals(90, creditEvaluationService.evaluate(1L).getScore());
    }

    @Test
    void evaluate_incomeBoundary_10000And5000() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(35, new BigDecimal("10000"), "GOOD", BigDecimal.ZERO));
        when(creditEvaluationMapper.selectOne(any())).thenReturn(null);

        assertEquals(90, creditEvaluationService.evaluate(1L).getScore());

        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(35, new BigDecimal("5000"), "GOOD", BigDecimal.ZERO));
        assertEquals(80, creditEvaluationService.evaluate(1L).getScore());
    }

    @Test
    void evaluate_debtRatioBoundary_30And50() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(creditEvaluationMapper.selectOne(any())).thenReturn(null);

        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(35, new BigDecimal("10000"), "GOOD", new BigDecimal("3000")));
        assertEquals(80, creditEvaluationService.evaluate(1L).getScore());

        when(sysUserMapper.selectById(1L)).thenReturn(buildUser(35, new BigDecimal("10000"), "GOOD", new BigDecimal("5000")));
        assertEquals(80, creditEvaluationService.evaluate(1L).getScore());
    }

    @Test
    void getEvaluation_withCustomerId_accessDenied() {
        LoanApplication application = buildApplication();
        application.setCustomerId(2L);
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> creditEvaluationService.getEvaluation(1L, 1L));
    }

    @Test
    void getEvaluation_notFound_shouldReturnNull() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(creditEvaluationMapper.selectOne(any())).thenReturn(null);

        assertNull(creditEvaluationService.getEvaluation(1L, 1L));
    }
}
