package com.bank.loan;

import com.bank.loan.dto.ApplicationMaterialDTO;
import com.bank.loan.dto.LoanApplicationDTO;
import com.bank.loan.dto.LoanApplicationDetailDTO;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.LoanProduct;
import com.bank.loan.entity.SysUser;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.mapper.ApplicationMaterialMapper;
import com.bank.loan.mapper.ApprovalRecordMapper;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.LoanProductMapper;
import com.bank.loan.service.CreditEvaluationService;
import com.bank.loan.service.DueDiligenceService;
import com.bank.loan.service.LoanProductService;
import com.bank.loan.service.SysUserService;
import com.bank.loan.service.impl.LoanApplicationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationMapper loanApplicationMapper;

    @Mock
    private ApplicationMaterialMapper applicationMaterialMapper;

    @Mock
    private LoanProductMapper loanProductMapper;

    @Mock
    private LoanProductService loanProductService;

    @Mock
    private CreditEvaluationService creditEvaluationService;

    @Mock
    private DueDiligenceService dueDiligenceService;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private ApprovalRecordMapper approvalRecordMapper;

    @InjectMocks
    private LoanApplicationServiceImpl loanApplicationService;

    private LoanApplication buildApplication(Long id, Long customerId, String status) {
        LoanApplication application = new LoanApplication();
        application.setId(id);
        application.setCustomerId(customerId);
        application.setStatus(status);
        return application;
    }

    private ApplicationMaterialDTO buildMaterial() {
        ApplicationMaterialDTO dto = new ApplicationMaterialDTO();
        dto.setMaterialType("INCOME_PROOF");
        dto.setFileName("income.pdf");
        dto.setFilePath("/files/income.pdf");
        return dto;
    }

    private ApplicationMaterialDTO buildMaterial(String type, String fileName, String filePath) {
        ApplicationMaterialDTO dto = new ApplicationMaterialDTO();
        dto.setMaterialType(type);
        dto.setFileName(fileName);
        dto.setFilePath(filePath);
        return dto;
    }

    private LoanProduct buildProduct() {
        LoanProduct product = new LoanProduct();
        product.setId(1L);
        product.setStatus(1);
        product.setMinAmount(new BigDecimal("10000"));
        product.setMaxAmount(new BigDecimal("500000"));
        product.setMinTerm(6);
        product.setMaxTerm(36);
        return product;
    }

    private LoanApplicationDTO buildSubmitRequest(List<ApplicationMaterialDTO> materials) {
        LoanApplicationDTO dto = new LoanApplicationDTO();
        dto.setProductId(1L);
        dto.setAmount(new BigDecimal("100000"));
        dto.setTermMonths(12);
        dto.setPurpose("装修");
        dto.setRepaymentSource("工资");
        dto.setNewMaterials(materials);
        return dto;
    }

    @Test
    void submitApplication_whenMaterialsMissing_shouldThrow() {
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct());

        LoanApplicationDTO request = buildSubmitRequest(List.of(
                buildMaterial("ID_CARD_FRONT", "front.jpg", "/download/a.jpg")
        ));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.submitApplication(1L, request));
        assertEquals("申请材料不完整，必须上传身份证正反面、收入证明、银行流水", ex.getMessage());
        verify(loanApplicationMapper, never()).insert(any());
    }

    @Test
    void submitApplication_whenMaterialsComplete_shouldInsert() {
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct());
        when(loanApplicationMapper.insert(any())).thenAnswer(invocation -> {
            LoanApplication app = invocation.getArgument(0);
            app.setId(1L);
            return 1;
        });
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication(1L, 1L, ApplicationStatus.SUBMITTED.name()));
        when(applicationMaterialMapper.selectList(any())).thenReturn(List.of());

        LoanApplicationDTO request = buildSubmitRequest(List.of(
                buildMaterial("ID_CARD_FRONT", "front.jpg", "/download/a.jpg"),
                buildMaterial("ID_CARD_BACK", "back.jpg", "/download/b.jpg"),
                buildMaterial("INCOME_PROOF", "income.pdf", "/download/c.pdf"),
                buildMaterial("BANK_STATEMENT", "bank.pdf", "/download/d.pdf")
        ));

        loanApplicationService.submitApplication(1L, request);

        verify(loanApplicationMapper).insert(any());
        verify(creditEvaluationService).evaluate(any());
    }

    @Test
    void addMaterials_whenSubmitted_shouldSaveOnly() {
        LoanApplication application = buildApplication(1L, 1L, ApplicationStatus.SUBMITTED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);

        loanApplicationService.addMaterials(1L, 1L, List.of(buildMaterial()));

        verify(applicationMaterialMapper).insert(any());
        verify(creditEvaluationService, never()).evaluate(any());
    }

    @Test
    void addMaterials_whenMaterialSupplement_shouldReEvaluate() {
        LoanApplication application = buildApplication(1L, 1L, ApplicationStatus.MATERIAL_SUPPLEMENT.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);

        loanApplicationService.addMaterials(1L, 1L, List.of(buildMaterial()));

        verify(applicationMaterialMapper).insert(any());
        verify(creditEvaluationService).evaluate(1L);
    }

    @Test
    void addMaterials_whenNotAllowedStatus_shouldThrow() {
        LoanApplication application = buildApplication(1L, 1L, ApplicationStatus.DUE_DILIGENCE.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.addMaterials(1L, 1L, List.of(buildMaterial())));
        assertEquals("当前申请状态不允许补充材料", ex.getMessage());
        verify(applicationMaterialMapper, never()).insert(any());
    }

    @Test
    void addMaterials_whenWrongCustomer_shouldThrow() {
        LoanApplication application = buildApplication(1L, 2L, ApplicationStatus.SUBMITTED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.addMaterials(1L, 1L, List.of(buildMaterial())));
        assertEquals("无权操作该申请", ex.getMessage());
    }

    @Test
    void submitApplication_productNotFound_shouldThrow() {
        when(loanProductMapper.selectById(1L)).thenReturn(null);

        LoanApplicationDTO request = buildSubmitRequest(List.of(buildMaterial()));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.submitApplication(1L, request));
        assertEquals("产品不存在", ex.getMessage());
    }

    @Test
    void submitApplication_productInactive_shouldThrow() {
        LoanProduct product = buildProduct();
        product.setStatus(0);
        when(loanProductMapper.selectById(1L)).thenReturn(product);

        LoanApplicationDTO request = buildSubmitRequest(List.of(buildMaterial()));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.submitApplication(1L, request));
        assertEquals("产品不可申请", ex.getMessage());
    }

    @Test
    void submitApplication_amountBelowMin_shouldThrow() {
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct());

        LoanApplicationDTO request = buildSubmitRequest(List.of(buildMaterial()));
        request.setAmount(new BigDecimal("1000"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.submitApplication(1L, request));
        assertTrue(ex.getMessage().contains("申请金额必须在产品额度范围内"));
    }

    @Test
    void submitApplication_amountAboveMax_shouldThrow() {
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct());

        LoanApplicationDTO request = buildSubmitRequest(List.of(buildMaterial()));
        request.setAmount(new BigDecimal("600000"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.submitApplication(1L, request));
        assertTrue(ex.getMessage().contains("申请金额必须在产品额度范围内"));
    }

    @Test
    void submitApplication_termBelowMin_shouldThrow() {
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct());

        LoanApplicationDTO request = buildSubmitRequest(List.of(buildMaterial()));
        request.setTermMonths(3);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.submitApplication(1L, request));
        assertTrue(ex.getMessage().contains("申请期限必须在产品期限范围内"));
    }

    @Test
    void submitApplication_termAboveMax_shouldThrow() {
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct());

        LoanApplicationDTO request = buildSubmitRequest(List.of(buildMaterial()));
        request.setTermMonths(48);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.submitApplication(1L, request));
        assertTrue(ex.getMessage().contains("申请期限必须在产品期限范围内"));
    }

    @Test
    void submitApplication_materialsEmpty_shouldThrow() {
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct());

        LoanApplicationDTO request = buildSubmitRequest(null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.submitApplication(1L, request));
        assertEquals("申请材料不能为空，请上传身份证正反面、收入证明、银行流水", ex.getMessage());
    }

    @Test
    void submitApplication_materialInvalid_shouldThrow() {
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct());

        LoanApplicationDTO request = buildSubmitRequest(List.of(
                buildMaterial("ID_CARD_FRONT", "front.jpg", ""),
                buildMaterial("ID_CARD_BACK", "back.jpg", "/download/b.jpg"),
                buildMaterial("INCOME_PROOF", "income.pdf", "/download/c.pdf"),
                buildMaterial("BANK_STATEMENT", "bank.pdf", "/download/d.pdf")
        ));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.submitApplication(1L, request));
        assertEquals("申请材料不完整，必须上传身份证正反面、收入证明、银行流水", ex.getMessage());
    }

    @Test
    void generateApplicationNo_whenFirstToday_shouldStartFromOne() {
        when(loanApplicationMapper.selectOne(any())).thenReturn(null);
        when(loanApplicationMapper.insert(any())).thenAnswer(invocation -> {
            LoanApplication app = invocation.getArgument(0);
            app.setId(1L);
            return 1;
        });
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication(1L, 1L, ApplicationStatus.SUBMITTED.name()));
        when(applicationMaterialMapper.selectList(any())).thenReturn(List.of());
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct());

        loanApplicationService.submitApplication(1L, buildSubmitRequest(List.of(
                buildMaterial("ID_CARD_FRONT", "front.jpg", "/download/a.jpg"),
                buildMaterial("ID_CARD_BACK", "back.jpg", "/download/b.jpg"),
                buildMaterial("INCOME_PROOF", "income.pdf", "/download/c.pdf"),
                buildMaterial("BANK_STATEMENT", "bank.pdf", "/download/d.pdf")
        )));

        ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(loanApplicationMapper).insert(captor.capture());
        assertTrue(captor.getValue().getApplicationNo().endsWith("0001"));
    }

    @Test
    void generateApplicationNo_sequenceIncrement() {
        LoanApplication last = new LoanApplication();
        last.setApplicationNo("LA" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "0003");
        when(loanApplicationMapper.selectOne(any())).thenReturn(last);
        when(loanApplicationMapper.insert(any())).thenAnswer(invocation -> {
            LoanApplication app = invocation.getArgument(0);
            app.setId(1L);
            return 1;
        });
        when(loanApplicationMapper.selectById(1L)).thenReturn(buildApplication(1L, 1L, ApplicationStatus.SUBMITTED.name()));
        when(applicationMaterialMapper.selectList(any())).thenReturn(List.of());
        when(loanProductMapper.selectById(1L)).thenReturn(buildProduct());

        loanApplicationService.submitApplication(1L, buildSubmitRequest(List.of(
                buildMaterial("ID_CARD_FRONT", "front.jpg", "/download/a.jpg"),
                buildMaterial("ID_CARD_BACK", "back.jpg", "/download/b.jpg"),
                buildMaterial("INCOME_PROOF", "income.pdf", "/download/c.pdf"),
                buildMaterial("BANK_STATEMENT", "bank.pdf", "/download/d.pdf")
        )));

        ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(loanApplicationMapper).insert(captor.capture());
        assertTrue(captor.getValue().getApplicationNo().endsWith("0004"));
    }

    @Test
    void getApplicationDetail_notFound_shouldThrow() {
        when(loanApplicationMapper.selectById(99L)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.getApplicationDetail(99L, 1L));
        assertEquals("申请不存在", ex.getMessage());
    }

    @Test
    void getApplicationDetail_unauthorized_shouldThrow() {
        LoanApplication application = buildApplication(1L, 2L, ApplicationStatus.SUBMITTED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.getApplicationDetail(1L, 1L));
        assertEquals("无权查看该申请", ex.getMessage());
    }

    @Test
    void getApplicationDetail_admin_shouldReturn() {
        LoanApplication application = buildApplication(1L, 2L, ApplicationStatus.SUBMITTED.name());
        application.setProductId(1L);
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(applicationMaterialMapper.selectList(any())).thenReturn(List.of());

        LoanApplicationDTO result = loanApplicationService.getApplicationDetail(1L, null);
        assertNotNull(result);
    }

    @Test
    void listApplications_withStatus_shouldFilter() {
        LoanApplication application = buildApplication(1L, 1L, ApplicationStatus.SUBMITTED.name());
        application.setProductId(1L);
        when(loanApplicationMapper.selectList(any())).thenReturn(List.of(application));
        when(applicationMaterialMapper.selectList(any())).thenReturn(List.of());

        List<LoanApplicationDTO> list = loanApplicationService.listApplications(ApplicationStatus.SUBMITTED.name());
        assertEquals(1, list.size());
    }

    @Test
    void listApplications_withoutStatus_shouldReturnAll() {
        LoanApplication application = buildApplication(1L, 1L, ApplicationStatus.SUBMITTED.name());
        application.setProductId(1L);
        when(loanApplicationMapper.selectList(any())).thenReturn(List.of(application));
        when(applicationMaterialMapper.selectList(any())).thenReturn(List.of());

        List<LoanApplicationDTO> list = loanApplicationService.listApplications("");
        assertEquals(1, list.size());
    }

    @Test
    void listAssignedApplications_shouldReturn() {
        LoanApplication application = buildApplication(1L, 1L, ApplicationStatus.SUBMITTED.name());
        application.setProductId(1L);
        when(loanApplicationMapper.selectList(any())).thenReturn(List.of(application));
        when(applicationMaterialMapper.selectList(any())).thenReturn(List.of());

        List<LoanApplicationDTO> list = loanApplicationService.listAssignedApplications(2L);
        assertEquals(1, list.size());
    }

    @Test
    void addMaterials_applicationNotFound_shouldThrow() {
        when(loanApplicationMapper.selectById(99L)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.addMaterials(99L, 1L, List.of(buildMaterial())));
        assertEquals("申请不存在", ex.getMessage());
    }

    @Test
    void addMaterials_emptyMaterials_shouldThrow() {
        LoanApplication application = buildApplication(1L, 1L, ApplicationStatus.SUBMITTED.name());
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanApplicationService.addMaterials(1L, 1L, List.of()));
        assertEquals("材料不能为空", ex.getMessage());
    }

    @Test
    void getApplicationFullDetail_shouldPopulateAll() {
        LoanApplication application = buildApplication(1L, 1L, ApplicationStatus.SUBMITTED.name());
        application.setProductId(1L);
        application.setAmount(new BigDecimal("100000"));
        application.setTermMonths(12);
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);
        when(applicationMaterialMapper.selectList(any())).thenReturn(List.of());

        SysUser customer = new SysUser();
        customer.setRealName("张三");
        customer.setPhone("13800138000");
        customer.setIdCard("110101199001011234");
        when(sysUserService.getById(1L)).thenReturn(customer);
        when(creditEvaluationService.getEvaluation(1L, null)).thenReturn(null);
        when(dueDiligenceService.getReport(1L)).thenReturn(null);
        when(approvalRecordMapper.selectList(any())).thenReturn(List.of());

        LoanApplicationDetailDTO detail = loanApplicationService.getApplicationFullDetail(1L);
        assertEquals("张三", detail.getCustomerName());
        assertEquals("138****8000", detail.getCustomerPhone());
        assertEquals("110***********1234", detail.getIdCard());
    }
}
