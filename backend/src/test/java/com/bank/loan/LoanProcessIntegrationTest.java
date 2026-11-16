package com.bank.loan;

import com.bank.loan.common.Result;
import com.bank.loan.dto.*;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.RepaymentScheduleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端业务流程集成测试
 *
 * 测试目标：从客户提交申请到放款生成还款计划的完整主流程及关键分支流程。
 * 本测试会写入独立的 loan_system_test.db，可在测试结束后查看真实业务数据。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoanProcessIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private LoanApplicationMapper loanApplicationMapper;

    @Autowired
    private RepaymentScheduleMapper repaymentScheduleMapper;

    private static final String CUSTOMER = "customer1";
    private static final String MANAGER = "manager1";
    private static final String APPROVER1 = "approver1";
    private static final String APPROVER2 = "approver2";
    private static final String COMMITTEE = "committee1";
    private static final String OFFICER = "officer1";
    private static final String PASSWORD = "123456";

    private static final Long PRODUCT_CREDIT = 1L;      // 个人信用贷 5k~50w
    private static final Long PRODUCT_BUSINESS = 2L;    // 个人经营贷 10w~200w
    private static final Long PRODUCT_HOUSE = 3L;       // 房屋抵押贷 20w~1000w

    @BeforeEach
    void setUp() {
        // 每次测试前清空非基础数据，避免状态互相影响
        repaymentScheduleMapper.delete(null);
        loanApplicationMapper.delete(null);
    }

    @Test
    void smallAmount_fullFlow_shouldDisburseAndGenerateRepaymentSchedule() {
        Long applicationId = submitApplication(CUSTOMER, PRODUCT_CREDIT, new BigDecimal("300000"), 12);

        assertStatus(applicationId, ApplicationStatus.CREDIT_EVALUATED);

        doDueDiligence(MANAGER, applicationId);
        assertStatus(applicationId, ApplicationStatus.DUE_DILIGENCE_COMPLETED);

        approve(APPROVER1, applicationId);
        assertStatus(applicationId, ApplicationStatus.CONTRACT_GENERATED);

        Long contractId = getContractId(CUSTOMER, applicationId);
        signContract(CUSTOMER, contractId);

        disburse(OFFICER, applicationId, new BigDecimal("300000"));
        assertStatus(applicationId, ApplicationStatus.DISBURSED);

        assertRepaymentScheduleExists(applicationId);
    }

    @Test
    void mediumAmount_fullFlow_shouldRequireTwoLevelsApproval() {
        Long applicationId = submitApplication(CUSTOMER, PRODUCT_BUSINESS, new BigDecimal("700000"), 24);

        doDueDiligence(MANAGER, applicationId);
        assertStatus(applicationId, ApplicationStatus.DUE_DILIGENCE_COMPLETED);

        approve(APPROVER1, applicationId);
        assertStatus(applicationId, ApplicationStatus.PENDING_APPROVAL);

        approve(APPROVER2, applicationId);
        assertStatus(applicationId, ApplicationStatus.CONTRACT_GENERATED);

        Long contractId = getContractId(CUSTOMER, applicationId);
        signContract(CUSTOMER, contractId);

        disburse(OFFICER, applicationId, new BigDecimal("700000"));
        assertStatus(applicationId, ApplicationStatus.DISBURSED);

        assertRepaymentScheduleExists(applicationId);
    }

    @Test
    void largeAmount_fullFlow_shouldRequireThreeLevelsApproval() {
        Long applicationId = submitApplication(CUSTOMER, PRODUCT_HOUSE, new BigDecimal("1500000"), 36);

        doDueDiligence(MANAGER, applicationId);
        assertStatus(applicationId, ApplicationStatus.DUE_DILIGENCE_COMPLETED);

        approve(APPROVER1, applicationId);
        assertStatus(applicationId, ApplicationStatus.PENDING_APPROVAL);

        approve(APPROVER2, applicationId);
        assertStatus(applicationId, ApplicationStatus.PENDING_APPROVAL);

        approve(COMMITTEE, applicationId);
        assertStatus(applicationId, ApplicationStatus.CONTRACT_GENERATED);

        Long contractId = getContractId(CUSTOMER, applicationId);
        signContract(CUSTOMER, contractId);

        disburse(OFFICER, applicationId, new BigDecimal("1500000"));
        assertStatus(applicationId, ApplicationStatus.DISBURSED);

        assertRepaymentScheduleExists(applicationId);
    }

    @Test
    void flow_rejectAtFirstLevel_shouldEndWithRejected() {
        Long applicationId = submitApplication(CUSTOMER, PRODUCT_CREDIT, new BigDecimal("200000"), 12);

        doDueDiligence(MANAGER, applicationId);

        reject(APPROVER1, applicationId);
        assertStatus(applicationId, ApplicationStatus.REJECTED);
    }

    @Test
    void flow_rejectAtSecondLevel_shouldEndWithRejected() {
        Long applicationId = submitApplication(CUSTOMER, PRODUCT_BUSINESS, new BigDecimal("600000"), 24);

        doDueDiligence(MANAGER, applicationId);
        approve(APPROVER1, applicationId);

        reject(APPROVER2, applicationId);
        assertStatus(applicationId, ApplicationStatus.REJECTED);
    }

    @Test
    void flow_managerSupplementThenReApprove_shouldEndWithApproved() {
        Long applicationId = submitApplication(CUSTOMER, PRODUCT_CREDIT, new BigDecimal("400000"), 12);

        doDueDiligenceWithConclusion(MANAGER, applicationId, "SUPPLEMENT");
        assertStatus(applicationId, ApplicationStatus.MATERIAL_SUPPLEMENT);

        addMaterials(CUSTOMER, applicationId);
        assertStatus(applicationId, ApplicationStatus.CREDIT_EVALUATED);

        doDueDiligence(MANAGER, applicationId);
        assertStatus(applicationId, ApplicationStatus.DUE_DILIGENCE_COMPLETED);

        approve(APPROVER1, applicationId);
        assertStatus(applicationId, ApplicationStatus.CONTRACT_GENERATED);
    }

    @Test
    void flow_approverSupplementThenReApprove_shouldEndWithApproved() {
        Long applicationId = submitApplication(CUSTOMER, PRODUCT_CREDIT, new BigDecimal("400000"), 12);

        doDueDiligence(MANAGER, applicationId);
        assertStatus(applicationId, ApplicationStatus.DUE_DILIGENCE_COMPLETED);

        supplement(APPROVER1, applicationId);
        assertStatus(applicationId, ApplicationStatus.MATERIAL_SUPPLEMENT);

        addMaterials(CUSTOMER, applicationId);
        assertStatus(applicationId, ApplicationStatus.CREDIT_EVALUATED);

        doDueDiligence(MANAGER, applicationId);
        assertStatus(applicationId, ApplicationStatus.DUE_DILIGENCE_COMPLETED);

        approve(APPROVER1, applicationId);
        assertStatus(applicationId, ApplicationStatus.CONTRACT_GENERATED);
    }

    @Test
    void flow_level1CannotApproveMediumAmount_shouldBeForbidden() {
        Long applicationId = submitApplication(CUSTOMER, PRODUCT_BUSINESS, new BigDecimal("700000"), 24);

        doDueDiligence(MANAGER, applicationId);

        HttpStatus status = tryApprove(APPROVER1, applicationId);
        assertEquals(HttpStatus.OK, status);

        // 一级审批通过后会进入 PENDING_APPROVAL，等待二级审批
        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        assertEquals(ApplicationStatus.PENDING_APPROVAL.name(), application.getStatus());
    }

    // ===================== 辅助方法 =====================

    private String login(String username) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(PASSWORD);

        ResponseEntity<Result<LoginResponse>> response = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<Result<LoginResponse>>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertNotNull(response.getBody().getData());
        return response.getBody().getData().getToken();
    }

    private HttpHeaders authHeader(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(login(username));
        return headers;
    }

    private Long submitApplication(String customer, Long productId, BigDecimal amount, int termMonths) {
        LoanApplicationDTO dto = new LoanApplicationDTO();
        dto.setProductId(productId);
        dto.setAmount(amount);
        dto.setTermMonths(termMonths);
        dto.setPurpose("经营周转");
        dto.setRepaymentSource("工资收入");

        List<ApplicationMaterialDTO> materials = new ArrayList<>();
        materials.add(buildMaterial("ID_CARD_FRONT", "身份证正面.jpg", "/uploads/id_front.jpg"));
        materials.add(buildMaterial("ID_CARD_BACK", "身份证反面.jpg", "/uploads/id_back.jpg"));
        materials.add(buildMaterial("INCOME_PROOF", "收入证明.pdf", "/uploads/income.pdf"));
        materials.add(buildMaterial("BANK_STATEMENT", "银行流水.pdf", "/uploads/statement.pdf"));
        dto.setNewMaterials(materials);

        ResponseEntity<Result<LoanApplicationDTO>> response = restTemplate.exchange(
                "/api/applications",
                HttpMethod.POST,
                new HttpEntity<>(dto, authHeader(customer)),
                new ParameterizedTypeReference<Result<LoanApplicationDTO>>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode(), response.getBody().getMsg());
        assertNotNull(response.getBody().getData());
        return response.getBody().getData().getId();
    }

    private ApplicationMaterialDTO buildMaterial(String type, String fileName, String filePath) {
        ApplicationMaterialDTO material = new ApplicationMaterialDTO();
        material.setMaterialType(type);
        material.setFileName(fileName);
        material.setFilePath(filePath);
        material.setUploadTime(LocalDateTime.now());
        return material;
    }

    private void addMaterials(String customer, Long applicationId) {
        List<ApplicationMaterialDTO> materials = new ArrayList<>();
        materials.add(buildMaterial("INCOME_PROOF", "补充收入证明.pdf", "/uploads/income2.pdf"));

        ResponseEntity<Result<Void>> response = restTemplate.exchange(
                "/api/applications/{id}/materials",
                HttpMethod.POST,
                new HttpEntity<>(materials, authHeader(customer)),
                new ParameterizedTypeReference<Result<Void>>() {},
                applicationId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode(), response.getBody().getMsg());
    }

    private void doDueDiligence(String manager, Long applicationId) {
        doDueDiligenceWithConclusion(manager, applicationId, "PASS");
    }

    private void doDueDiligenceWithConclusion(String manager, Long applicationId, String conclusion) {
        DueDiligenceDTO dto = new DueDiligenceDTO();
        dto.setBusinessSituation("客户经营稳定，流水正常");
        dto.setAssetsLiabilities("资产大于负债，偿债能力良好");
        dto.setRepaymentAbility("月收入可覆盖月供，还款能力强");
        dto.setConclusion(conclusion);

        ResponseEntity<Result<DueDiligenceDTO>> response = restTemplate.exchange(
                "/api/due-diligence/{applicationId}",
                HttpMethod.POST,
                new HttpEntity<>(dto, authHeader(manager)),
                new ParameterizedTypeReference<Result<DueDiligenceDTO>>() {},
                applicationId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode(), response.getBody().getMsg());
    }

    private void approve(String approver, Long applicationId) {
        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setAction("APPROVE");
        request.setComment("同意");

        ResponseEntity<Result<Void>> response = restTemplate.exchange(
                "/api/approvals/{applicationId}",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeader(approver)),
                new ParameterizedTypeReference<Result<Void>>() {},
                applicationId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode(), response.getBody().getMsg());
    }

    private void reject(String approver, Long applicationId) {
        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setAction("REJECT");
        request.setComment("资质不符");

        ResponseEntity<Result<Void>> response = restTemplate.exchange(
                "/api/approvals/{applicationId}",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeader(approver)),
                new ParameterizedTypeReference<Result<Void>>() {},
                applicationId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode(), response.getBody().getMsg());
    }

    private void supplement(String approver, Long applicationId) {
        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setAction("SUPPLEMENT");
        request.setComment("请补充收入证明");

        ResponseEntity<Result<Void>> response = restTemplate.exchange(
                "/api/approvals/{applicationId}",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeader(approver)),
                new ParameterizedTypeReference<Result<Void>>() {},
                applicationId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode(), response.getBody().getMsg());
    }

    private HttpStatus tryApprove(String approver, Long applicationId) {
        ApprovalActionRequest request = new ApprovalActionRequest();
        request.setAction("APPROVE");
        request.setComment("同意");

        ResponseEntity<Result<Void>> response = restTemplate.exchange(
                "/api/approvals/{applicationId}",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeader(approver)),
                new ParameterizedTypeReference<Result<Void>>() {},
                applicationId);

        return response.getStatusCode();
    }

    private Long getContractId(String customer, Long applicationId) {
        ResponseEntity<Result<LoanContractDTO>> response = restTemplate.exchange(
                "/api/contracts/{applicationId}",
                HttpMethod.GET,
                new HttpEntity<>(authHeader(customer)),
                new ParameterizedTypeReference<Result<LoanContractDTO>>() {},
                applicationId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode(), response.getBody().getMsg());
        assertNotNull(response.getBody().getData());
        return response.getBody().getData().getId();
    }

    private void signContract(String customer, Long contractId) {
        ResponseEntity<Result<LoanContractDTO>> response = restTemplate.exchange(
                "/api/contracts/sign/{contractId}",
                HttpMethod.POST,
                new HttpEntity<>(authHeader(customer)),
                new ParameterizedTypeReference<Result<LoanContractDTO>>() {},
                contractId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode(), response.getBody().getMsg());
    }

    private void disburse(String officer, Long applicationId, BigDecimal amount) {
        LoanDisbursementDTO dto = new LoanDisbursementDTO();
        dto.setAmount(amount);
        dto.setAccountNo("6222021234567890123");
        dto.setBankName("工商银行");
        dto.setDisbursementTime(LocalDateTime.now());

        ResponseEntity<Result<LoanDisbursementDTO>> response = restTemplate.exchange(
                "/api/disbursements/{applicationId}",
                HttpMethod.POST,
                new HttpEntity<>(dto, authHeader(officer)),
                new ParameterizedTypeReference<Result<LoanDisbursementDTO>>() {},
                applicationId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode(), response.getBody().getMsg());
    }

    private void assertStatus(Long applicationId, ApplicationStatus expected) {
        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        assertNotNull(application, "申请不存在");
        assertEquals(expected.name(), application.getStatus());
    }

    private void assertRepaymentScheduleExists(Long applicationId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.bank.loan.entity.RepaymentSchedule> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("application_id", applicationId);
        long count = repaymentScheduleMapper.selectCount(wrapper);
        assertTrue(count > 0, "应生成还款计划");
    }
}
