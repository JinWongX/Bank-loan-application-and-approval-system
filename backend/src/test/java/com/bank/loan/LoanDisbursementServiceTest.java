package com.bank.loan;

import com.bank.loan.dto.LoanApplicationDTO;
import com.bank.loan.dto.LoanDisbursementDTO;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.LoanDisbursement;
import com.bank.loan.entity.SysUser;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.LoanDisbursementMapper;
import com.bank.loan.mapper.SysUserMapper;
import com.bank.loan.service.LoanApplicationService;
import com.bank.loan.service.RepaymentScheduleService;
import com.bank.loan.service.impl.LoanDisbursementServiceImpl;
import com.bank.loan.util.AesUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanDisbursementServiceTest {

    @Mock
    private LoanApplicationMapper loanApplicationMapper;

    @Mock
    private LoanDisbursementMapper loanDisbursementMapper;

    @Mock
    private LoanApplicationService loanApplicationService;

    @Mock
    private RepaymentScheduleService repaymentScheduleService;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private AesUtil aesUtil;

    @InjectMocks
    private LoanDisbursementServiceImpl loanDisbursementService;

    private LoanApplication buildApplication(BigDecimal amount) {
        LoanApplication application = new LoanApplication();
        application.setId(1L);
        application.setCustomerId(1L);
        application.setAmount(amount);
        application.setTermMonths(12);
        application.setStatus(ApplicationStatus.SIGNED.name());
        return application;
    }

    @Test
    void disburse_amountMismatch_shouldThrow() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication(new BigDecimal("100000")));

        LoanDisbursementDTO dto = new LoanDisbursementDTO();
        dto.setAmount(new BigDecimal("99999"));
        dto.setAccountNo("6222021234567890123");
        dto.setBankName("测试银行");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanDisbursementService.disburse(1L, dto, 10L));
        assertEquals("放款金额必须等于申请金额：100000", ex.getMessage());
        verify(loanDisbursementMapper, never()).insert(any());
        verify(repaymentScheduleService, never()).generateSchedule(any());
    }

    @Test
    void disburse_amountMatch_shouldSuccess() {
        LoanApplication application = buildApplication(new BigDecimal("100000"));
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(aesUtil.encrypt(any())).thenReturn("encrypted");

        LoanDisbursementDTO dto = new LoanDisbursementDTO();
        dto.setAmount(new BigDecimal("100000"));
        dto.setAccountNo("6222021234567890123");
        dto.setBankName("测试银行");

        loanDisbursementService.disburse(1L, dto, 10L);

        assertEquals(ApplicationStatus.DISBURSED.name(), application.getStatus());
        verify(loanDisbursementMapper).insert(any());
        verify(repaymentScheduleService).generateSchedule(1L);
    }

    @Test
    void listPendingApplications_shouldReturnSignedApplications() {
        LoanApplicationDTO dto = new LoanApplicationDTO();
        dto.setId(1L);
        dto.setCustomerId(1L);
        when(loanApplicationService.listApplications(ApplicationStatus.SIGNED.name())).thenReturn(List.of(dto));
        SysUser user = new SysUser();
        user.setRealName("张三");
        when(sysUserMapper.selectById(1L)).thenReturn(user);

        List<LoanApplicationDTO> list = loanDisbursementService.listPendingApplications();
        assertEquals(1, list.size());
        assertEquals("张三", list.get(0).getCustomerName());
    }

    @Test
    void disburse_applicationNotFound_shouldThrow() {
        when(loanApplicationMapper.selectById(99L)).thenReturn(null);
        LoanDisbursementDTO dto = new LoanDisbursementDTO();
        dto.setAmount(new BigDecimal("100000"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanDisbursementService.disburse(99L, dto, 10L));
        assertEquals("申请不存在", ex.getMessage());
    }

    @Test
    void disburse_notSigned_shouldThrow() {
        LoanApplication application = buildApplication(new BigDecimal("100000"));
        application.setStatus(ApplicationStatus.CONTRACT_GENERATED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        LoanDisbursementDTO dto = new LoanDisbursementDTO();
        dto.setAmount(new BigDecimal("100000"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanDisbursementService.disburse(1L, dto, 10L));
        assertEquals("当前申请状态不允许放款", ex.getMessage());
    }

    @Test
    void disburse_amountNotPositive_shouldThrow() {
        LoanApplication application = buildApplication(new BigDecimal("100000"));
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        LoanDisbursementDTO dto = new LoanDisbursementDTO();
        dto.setAmount(BigDecimal.ZERO);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanDisbursementService.disburse(1L, dto, 10L));
        assertEquals("放款金额必须大于0", ex.getMessage());
    }

    @Test
    void disburse_withOptionalTime_shouldUseProvidedTime() {
        LoanApplication application = buildApplication(new BigDecimal("100000"));
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(aesUtil.encrypt(any())).thenReturn("encrypted");

        LocalDateTime specificTime = LocalDateTime.of(2026, 9, 1, 10, 0);
        LoanDisbursementDTO dto = new LoanDisbursementDTO();
        dto.setAmount(new BigDecimal("100000"));
        dto.setAccountNo("6222021234567890123");
        dto.setBankName("测试银行");
        dto.setDisbursementTime(specificTime);

        loanDisbursementService.disburse(1L, dto, 10L);

        assertEquals(ApplicationStatus.DISBURSED.name(), application.getStatus());
        verify(loanDisbursementMapper).insert(any());
    }

    @Test
    void getDisbursementByApplicationId_notFound_shouldReturnNull() {
        when(loanDisbursementMapper.selectOne(any())).thenReturn(null);
        assertNull(loanDisbursementService.getDisbursementByApplicationId(1L));
    }

    @Test
    void getDisbursementByApplicationId_found_shouldReturnDto() {
        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setId(1L);
        disbursement.setApplicationId(1L);
        disbursement.setAmount(new BigDecimal("100000"));
        disbursement.setAccountNo("encrypted");
        when(loanDisbursementMapper.selectOne(any())).thenReturn(disbursement);
        when(aesUtil.decrypt("encrypted")).thenReturn("6222021234567890123");

        LoanDisbursementDTO dto = loanDisbursementService.getDisbursementByApplicationId(1L);
        assertNotNull(dto);
        assertEquals(new BigDecimal("100000"), dto.getAmount());
    }
}
