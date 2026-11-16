package com.bank.loan;

import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.LoanDisbursement;
import com.bank.loan.entity.LoanProduct;
import com.bank.loan.entity.RepaymentSchedule;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.LoanDisbursementMapper;
import com.bank.loan.mapper.LoanProductMapper;
import com.bank.loan.mapper.RepaymentScheduleMapper;
import com.bank.loan.service.impl.RepaymentScheduleServiceImpl;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RepaymentScheduleServiceTest {

    @Mock
    private LoanApplicationMapper loanApplicationMapper;

    @Mock
    private LoanProductMapper loanProductMapper;

    @Mock
    private LoanDisbursementMapper loanDisbursementMapper;

    @Mock
    private RepaymentScheduleMapper repaymentScheduleMapper;

    @InjectMocks
    private RepaymentScheduleServiceImpl repaymentScheduleService;

    private List<RepaymentSchedule> generatedSchedules;

    @BeforeEach
    void setUp() {
        generatedSchedules = new ArrayList<>();
        when(repaymentScheduleMapper.insert(any(RepaymentSchedule.class))).thenAnswer(invocation -> {
            RepaymentSchedule schedule = invocation.getArgument(0);
            generatedSchedules.add(schedule);
            return 1;
        });
    }

    private LoanApplication buildApplication() {
        LoanApplication application = new LoanApplication();
        application.setId(1L);
        application.setAmount(new BigDecimal("120000"));
        application.setTermMonths(12);
        application.setProductId(1L);
        return application;
    }

    private LoanProduct buildProduct(String repaymentMethod) {
        LoanProduct product = new LoanProduct();
        product.setId(1L);
        product.setName("测试产品");
        product.setInterestRate(new BigDecimal("0.06"));
        product.setRepaymentMethod(repaymentMethod);
        return product;
    }

    private void mockDependencies(String repaymentMethod) {
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct(repaymentMethod));
        when(loanDisbursementMapper.selectOne(any())).thenReturn(null);
    }

    @Test
    void generateSchedule_equalPrincipalInterest() {
        mockDependencies("等额本息");

        repaymentScheduleService.generateSchedule(1L);

        assertEquals(12, generatedSchedules.size());
        BigDecimal totalPrincipal = generatedSchedules.stream()
                .map(RepaymentSchedule::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, totalPrincipal.compareTo(new BigDecimal("120000")));
        assertTrue(generatedSchedules.get(11).getRemainingPrincipal().compareTo(BigDecimal.ZERO) <= 0);
    }

    @Test
    void generateSchedule_equalPrincipal() {
        mockDependencies("等额本金");

        repaymentScheduleService.generateSchedule(1L);

        assertEquals(12, generatedSchedules.size());
        BigDecimal totalPrincipal = generatedSchedules.stream()
                .map(RepaymentSchedule::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, totalPrincipal.compareTo(new BigDecimal("120000")));

        BigDecimal basePrincipal = new BigDecimal("10000");
        assertEquals(0, generatedSchedules.get(0).getPrincipal().compareTo(basePrincipal));
        assertTrue(generatedSchedules.get(0).getInterest().compareTo(generatedSchedules.get(1).getInterest()) > 0);
    }

    @Test
    void generateSchedule_interestFirst() {
        mockDependencies("先息后本");

        repaymentScheduleService.generateSchedule(1L);

        assertEquals(12, generatedSchedules.size());
        for (int i = 0; i < 11; i++) {
            assertEquals(0, generatedSchedules.get(i).getPrincipal().compareTo(BigDecimal.ZERO));
        }
        assertEquals(0, generatedSchedules.get(11).getPrincipal().compareTo(new BigDecimal("120000")));
        assertEquals(0, generatedSchedules.get(11).getRemainingPrincipal().compareTo(BigDecimal.ZERO));
    }

    @Test
    void generateSchedule_lumpSum() {
        mockDependencies("到期一次性还本付息");

        repaymentScheduleService.generateSchedule(1L);

        assertEquals(1, generatedSchedules.size());
        RepaymentSchedule schedule = generatedSchedules.get(0);
        assertEquals(0, schedule.getPrincipal().compareTo(new BigDecimal("120000")));
        assertEquals(0, schedule.getInterest().compareTo(new BigDecimal("7200")));
        assertEquals(0, schedule.getTotalAmount().compareTo(new BigDecimal("127200")));
        assertEquals(0, schedule.getRemainingPrincipal().compareTo(BigDecimal.ZERO));
    }

    @Test
    void generateSchedule_withDisbursementDate() {
        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setDisbursementTime(LocalDateTime.of(2026, 9, 1, 10, 0));
        when(loanDisbursementMapper.selectOne(any())).thenReturn(disbursement);
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication());
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct("等额本息"));

        repaymentScheduleService.generateSchedule(1L);

        assertEquals(LocalDate.of(2026, 10, 1), generatedSchedules.get(0).getDueDate());
    }

    @Test
    void generateSchedule_invalidAmount_shouldThrow() {
        LoanApplication application = buildApplication();
        application.setAmount(new BigDecimal("-1000"));
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct("等额本息"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> repaymentScheduleService.generateSchedule(1L));
        assertEquals("贷款金额不合法", ex.getMessage());
    }

    @Test
    void generateSchedule_invalidTerm_shouldThrow() {
        LoanApplication application = buildApplication();
        application.setTermMonths(0);
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct("等额本息"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> repaymentScheduleService.generateSchedule(1L));
        assertEquals("贷款期限不合法", ex.getMessage());
    }

    @Test
    void generateSchedule_quarterlyInterest() {
        mockDependencies("按季付息到期还本");

        repaymentScheduleService.generateSchedule(1L);

        assertEquals(12, generatedSchedules.size());
        BigDecimal totalPrincipal = generatedSchedules.stream()
                .map(RepaymentSchedule::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, totalPrincipal.compareTo(new BigDecimal("120000")));
        assertTrue(generatedSchedules.get(2).getInterest().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(generatedSchedules.get(11).getRemainingPrincipal().compareTo(BigDecimal.ZERO) <= 0);
    }

    @Test
    void generateSchedule_unknownMethod_shouldFallbackToLumpSum() {
        LoanApplication application = buildApplication();
        application.setTermMonths(6);
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        LoanProduct product = buildProduct("未知还款方式");
        when(loanProductMapper.selectById(1L)).thenReturn(product);
        when(loanDisbursementMapper.selectOne(any())).thenReturn(null);

        repaymentScheduleService.generateSchedule(1L);

        assertEquals(1, generatedSchedules.size());
        assertEquals(0, generatedSchedules.get(0).getPrincipal().compareTo(new BigDecimal("120000")));
    }

    @Test
    void listByApplicationId_empty_shouldReturnEmpty() {
        when(repaymentScheduleMapper.selectList(any())).thenReturn(List.of());
        List<com.bank.loan.dto.RepaymentScheduleDTO> list = repaymentScheduleService.listByApplicationId(1L);
        assertTrue(list.isEmpty());
    }

    @Test
    void listByApplicationId_nonEmpty_shouldReturnDtos() {
        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setId(1L);
        schedule.setApplicationId(1L);
        schedule.setPeriod(1);
        schedule.setPrincipal(new BigDecimal("10000"));
        schedule.setInterest(new BigDecimal("100"));
        schedule.setTotalAmount(new BigDecimal("10100"));
        schedule.setRemainingPrincipal(new BigDecimal("110000"));
        when(repaymentScheduleMapper.selectList(any())).thenReturn(List.of(schedule));

        List<com.bank.loan.dto.RepaymentScheduleDTO> list = repaymentScheduleService.listByApplicationId(1L);
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).getPeriod());
    }
}
