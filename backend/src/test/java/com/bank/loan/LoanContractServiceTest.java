package com.bank.loan;

import com.bank.loan.dto.LoanApplicationDetailDTO;
import com.bank.loan.dto.LoanContractDTO;
import com.bank.loan.dto.LoanProductDTO;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.LoanContract;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.LoanContractMapper;
import com.bank.loan.service.LoanApplicationService;
import com.bank.loan.service.impl.LoanContractServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanContractServiceTest {

    @Mock
    private LoanApplicationMapper loanApplicationMapper;

    @Mock
    private LoanContractMapper loanContractMapper;

    @Mock
    private LoanApplicationService loanApplicationService;

    @InjectMocks
    private LoanContractServiceImpl loanContractService;

    private LoanApplication buildApplication(Long id, String status) {
        LoanApplication application = new LoanApplication();
        application.setId(id);
        application.setCustomerId(1L);
        application.setApplicationNo("LA20260901000" + id);
        application.setProductId(1L);
        application.setAmount(new BigDecimal("100000"));
        application.setTermMonths(12);
        application.setStatus(status);
        return application;
    }

    private LoanApplicationDetailDTO buildDetail() {
        LoanApplicationDetailDTO detail = new LoanApplicationDetailDTO();
        detail.setCustomerName("张三");
        detail.setIdCard("110101199001011234");
        detail.setAmount(new BigDecimal("100000"));
        detail.setTermMonths(12);

        LoanProductDTO product = new LoanProductDTO();
        product.setId(1L);
        product.setName("个人信用贷");
        product.setInterestRate(new BigDecimal("0.045"));
        product.setRepaymentMethod("等额本息");
        detail.setProduct(product);
        return detail;
    }

    private LoanContract buildContract(Long id, Long applicationId, String signStatus) {
        LoanContract contract = new LoanContract();
        contract.setId(id);
        contract.setApplicationId(applicationId);
        contract.setContractNo("CT202609010001");
        contract.setContractContent("测试合同内容");
        contract.setSignStatus(signStatus);
        return contract;
    }

    @Test
    void generateContract_whenApplicationApprovedAndNoExisting_shouldGenerate() {
        LoanApplication application = buildApplication(1L, ApplicationStatus.APPROVED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(loanContractMapper.selectOne(any())).thenReturn(null);
        when(loanApplicationService.getApplicationFullDetail(1L)).thenReturn(buildDetail());
        when(loanContractMapper.insert(any(LoanContract.class))).thenAnswer(invocation -> {
            LoanContract c = invocation.getArgument(0);
            c.setId(1L);
            return 1;
        });

        LoanContractDTO dto = loanContractService.generateContract(1L);

        assertNotNull(dto);
        assertEquals(1L, dto.getApplicationId());
        assertEquals("UNSIGNED", dto.getSignStatus());
        assertTrue(dto.getContractContent().contains("贷款金额（大写）"));
        verify(loanApplicationMapper).updateById(application);
        assertEquals(ApplicationStatus.CONTRACT_GENERATED.name(), application.getStatus());
    }

    @Test
    void generateContract_whenApplicationNotExist_shouldThrow() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanContractService.generateContract(1L));
        assertEquals("申请不存在", ex.getMessage());
    }

    @Test
    void generateContract_whenApplicationNotApproved_shouldThrow() {
        LoanApplication application = buildApplication(1L, ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanContractService.generateContract(1L));
        assertEquals("当前申请未审批通过，无法生成合同", ex.getMessage());
    }

    @Test
    void getContractByApplicationId_whenOwnApplication_shouldReturn() {
        LoanApplication application = buildApplication(2L, ApplicationStatus.CONTRACT_GENERATED.name());
        LoanContract contract = buildContract(1L, 2L, "UNSIGNED");
        when(loanApplicationMapper.selectById(2L)).thenReturn(application);
        when(loanContractMapper.selectOne(any())).thenReturn(contract);

        LoanContractDTO dto = loanContractService.getContractByApplicationId(2L, 1L);

        assertNotNull(dto);
        assertEquals(2L, dto.getApplicationId());
        assertEquals("UNSIGNED", dto.getSignStatus());
    }

    @Test
    void signContract_whenOwnAndUnsigned_shouldSign() {
        LoanContract contract = buildContract(1L, 2L, "UNSIGNED");
        LoanApplication application = buildApplication(2L, ApplicationStatus.CONTRACT_GENERATED.name());
        application.setCustomerId(1L);
        when(loanContractMapper.selectById(1L)).thenReturn(contract);
        when(loanApplicationMapper.selectById(2L)).thenReturn(application);

        LoanContractDTO dto = loanContractService.signContract(1L, 1L);

        assertNotNull(dto);
        assertEquals("SIGNED", dto.getSignStatus());
        assertNotNull(dto.getSignTime());
        assertEquals(ApplicationStatus.SIGNED.name(), application.getStatus());
        verify(loanContractMapper).updateById(contract);
        verify(loanApplicationMapper).updateById(application);
    }

    @Test
    void signContract_whenNotOwn_shouldThrow() {
        LoanContract contract = buildContract(1L, 2L, "UNSIGNED");
        LoanApplication application = buildApplication(2L, ApplicationStatus.CONTRACT_GENERATED.name());
        application.setCustomerId(1L);
        when(loanContractMapper.selectById(1L)).thenReturn(contract);
        when(loanApplicationMapper.selectById(2L)).thenReturn(application);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanContractService.signContract(1L, 2L));
        assertEquals("只有申请人本人才能签署合同", ex.getMessage());
    }

    @Test
    void signContract_whenAlreadySigned_shouldThrow() {
        LoanContract contract = buildContract(1L, 2L, "SIGNED");
        LoanApplication application = buildApplication(2L, ApplicationStatus.SIGNED.name());
        application.setCustomerId(1L);
        when(loanContractMapper.selectById(1L)).thenReturn(contract);
        when(loanApplicationMapper.selectById(2L)).thenReturn(application);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanContractService.signContract(1L, 1L));
        assertEquals("合同已签署或状态异常", ex.getMessage());
    }

    @Test
    void generateContract_contentContainsKeyFields() {
        LoanApplication application = buildApplication(3L, ApplicationStatus.APPROVED.name());
        when(loanApplicationMapper.selectById(3L)).thenReturn(application);
        when(loanContractMapper.selectOne(any())).thenReturn(null);
        when(loanApplicationService.getApplicationFullDetail(3L)).thenReturn(buildDetail());
        when(loanContractMapper.insert(any(LoanContract.class))).thenAnswer(invocation -> {
            LoanContract c = invocation.getArgument(0);
            c.setId(3L);
            return 1;
        });

        LoanContractDTO dto = loanContractService.generateContract(3L);

        String content = dto.getContractContent();
        assertTrue(content.contains("贷款金额（大写）"));
        assertTrue(content.contains("年利率"));
        assertTrue(content.contains("还款方式"));
        assertTrue(content.contains("个人信用贷"));
        assertTrue(content.contains("等额本息"));
        assertTrue(content.contains("壹拾万元整") || content.contains("拾万元整"));
        assertTrue(content.contains("4.50%"));
    }

    @Test
    void listMyContracts_whenHasContracts_shouldReturn() {
        LoanApplication app1 = buildApplication(2L, ApplicationStatus.CONTRACT_GENERATED.name());
        LoanApplication app2 = buildApplication(3L, ApplicationStatus.SIGNED.name());
        when(loanApplicationMapper.selectList(any())).thenReturn(List.of(app1, app2));
        LoanContract contract1 = buildContract(1L, 2L, "UNSIGNED");
        LoanContract contract2 = buildContract(2L, 3L, "SIGNED");
        when(loanContractMapper.selectList(any())).thenReturn(List.of(contract1, contract2));

        List<LoanContractDTO> list = loanContractService.listMyContracts(1L);

        assertEquals(2, list.size());
    }

    @Test
    void listMyContracts_whenNoContracts_shouldReturnEmpty() {
        when(loanApplicationMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<LoanContractDTO> list = loanContractService.listMyContracts(1L);

        assertTrue(list.isEmpty());
    }

    @Test
    void generateContract_whenExisting_shouldReturnExisting() {
        LoanApplication application = buildApplication(1L, ApplicationStatus.APPROVED.name());
        LoanContract existing = buildContract(1L, 1L, "UNSIGNED");
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(loanContractMapper.selectOne(any())).thenReturn(existing);

        LoanContractDTO dto = loanContractService.generateContract(1L);

        assertEquals("UNSIGNED", dto.getSignStatus());
        verify(loanContractMapper, never()).insert(any());
    }

    @Test
    void getContractByApplicationId_notFound_shouldThrow() {
        when(loanApplicationMapper.selectById(1L)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanContractService.getContractByApplicationId(1L, 1L));
        assertEquals("申请不存在", ex.getMessage());
    }

    @Test
    void getContractByApplicationId_unauthorized_shouldThrow() {
        LoanApplication application = buildApplication(2L, ApplicationStatus.CONTRACT_GENERATED.name());
        application.setCustomerId(1L);
        when(loanApplicationMapper.selectById(2L)).thenReturn(application);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanContractService.getContractByApplicationId(2L, 2L));
        assertEquals("无权查看该合同", ex.getMessage());
    }

    @Test
    void generateContract_contentWithJiaoFen() {
        LoanApplication application = buildApplication(5L, ApplicationStatus.APPROVED.name());
        when(loanApplicationMapper.selectById(5L)).thenReturn(application);
        when(loanContractMapper.selectOne(any())).thenReturn(null);
        LoanApplicationDetailDTO detail = buildDetail();
        detail.setAmount(new BigDecimal("100000.56"));
        when(loanApplicationService.getApplicationFullDetail(5L)).thenReturn(detail);
        when(loanContractMapper.insert(any(LoanContract.class))).thenAnswer(invocation -> {
            LoanContract c = invocation.getArgument(0);
            c.setId(5L);
            return 1;
        });

        LoanContractDTO dto = loanContractService.generateContract(5L);
        String content = dto.getContractContent();
        assertTrue(content.contains("壹拾万元伍角陆分") || content.contains("拾万元伍角陆分"));
    }

    @Test
    void generateContract_contentWithEmptyFields() {
        LoanApplication application = buildApplication(6L, ApplicationStatus.APPROVED.name());
        when(loanApplicationMapper.selectById(6L)).thenReturn(application);
        when(loanContractMapper.selectOne(any())).thenReturn(null);
        LoanApplicationDetailDTO detail = new LoanApplicationDetailDTO();
        detail.setAmount(new BigDecimal("0"));
        detail.setTermMonths(0);
        detail.setProduct(null);
        when(loanApplicationService.getApplicationFullDetail(6L)).thenReturn(detail);
        when(loanContractMapper.insert(any(LoanContract.class))).thenAnswer(invocation -> {
            LoanContract c = invocation.getArgument(0);
            c.setId(6L);
            return 1;
        });

        LoanContractDTO dto = loanContractService.generateContract(6L);
        String content = dto.getContractContent();
        assertTrue(content.contains("零元整"));
        assertTrue(content.contains("年利率：0.00%\n"));
    }

    @Test
    void generateContract_contentWithNullRate() {
        LoanApplication application = buildApplication(7L, ApplicationStatus.APPROVED.name());
        when(loanApplicationMapper.selectById(7L)).thenReturn(application);
        when(loanContractMapper.selectOne(any())).thenReturn(null);
        LoanApplicationDetailDTO detail = buildDetail();
        detail.getProduct().setInterestRate(null);
        when(loanApplicationService.getApplicationFullDetail(7L)).thenReturn(detail);
        when(loanContractMapper.insert(any(LoanContract.class))).thenAnswer(invocation -> {
            LoanContract c = invocation.getArgument(0);
            c.setId(7L);
            return 1;
        });

        LoanContractDTO dto = loanContractService.generateContract(7L);
        assertTrue(dto.getContractContent().contains("年利率：-\n"));
    }

    @Test
    void listMyContracts_whenNoMatchedContracts_shouldReturnEmpty() {
        LoanApplication app = buildApplication(2L, ApplicationStatus.CONTRACT_GENERATED.name());
        when(loanApplicationMapper.selectList(any())).thenReturn(List.of(app));
        when(loanContractMapper.selectList(any())).thenReturn(List.of(buildContract(1L, 99L, "UNSIGNED")));

        List<LoanContractDTO> list = loanContractService.listMyContracts(1L);
        assertTrue(list.isEmpty());
    }
}
