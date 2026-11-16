package com.bank.loan;

import com.bank.loan.config.JwtConfig;
import com.bank.loan.dto.LoanApplicationDTO;
import com.bank.loan.dto.LoanDisbursementDTO;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.service.LoanDisbursementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoanDisbursementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtConfig jwtConfig;

    @MockBean
    private LoanDisbursementService loanDisbursementService;

    @MockBean
    private LoanApplicationMapper loanApplicationMapper;

    private String officerToken() {
        return jwtConfig.generateToken(6L, "officer1", "disbursement_officer");
    }

    private String customerToken() {
        return jwtConfig.generateToken(1L, "customer1", "customer");
    }

    @Test
    void listPendingApplications_success() throws Exception {
        LoanApplicationDTO dto = new LoanApplicationDTO();
        dto.setId(3L);
        dto.setApplicationNo("LA202609010003");
        when(loanDisbursementService.listPendingApplications()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/disbursements/pending")
                        .header("Authorization", "Bearer " + officerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(3));
    }

    @Test
    void listPendingApplications_customerShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/disbursements/pending")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void disburse_success() throws Exception {
        LoanDisbursementDTO result = new LoanDisbursementDTO();
        result.setId(1L);
        result.setApplicationId(3L);
        result.setAmount(new BigDecimal("50000"));
        when(loanDisbursementService.disburse(eq(3L), any(LoanDisbursementDTO.class), eq(6L))).thenReturn(result);

        LoanDisbursementDTO request = new LoanDisbursementDTO();
        request.setAmount(new BigDecimal("50000"));
        request.setAccountNo("6222021234567890123");
        request.setBankName("工商银行");

        mockMvc.perform(post("/api/disbursements/3")
                        .header("Authorization", "Bearer " + officerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.amount").value(50000));
    }

    @Test
    void getDisbursement_officer_shouldSuccess() throws Exception {
        LoanDisbursementDTO dto = new LoanDisbursementDTO();
        dto.setId(1L);
        dto.setApplicationId(4L);
        when(loanDisbursementService.getDisbursementByApplicationId(4L)).thenReturn(dto);

        mockMvc.perform(get("/api/disbursements/4")
                        .header("Authorization", "Bearer " + officerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getDisbursement_customerOwn_shouldSuccess() throws Exception {
        LoanApplication application = new LoanApplication();
        application.setId(3L);
        application.setCustomerId(1L);
        when(loanApplicationMapper.selectById(3L)).thenReturn(application);

        LoanDisbursementDTO dto = new LoanDisbursementDTO();
        dto.setId(2L);
        when(loanDisbursementService.getDisbursementByApplicationId(3L)).thenReturn(dto);

        mockMvc.perform(get("/api/disbursements/3")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    void getDisbursement_customerNotOwn_shouldReturnError() throws Exception {
        LoanApplication application = new LoanApplication();
        application.setId(3L);
        application.setCustomerId(2L);
        when(loanApplicationMapper.selectById(3L)).thenReturn(application);

        mockMvc.perform(get("/api/disbursements/3")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("无权查看该放款信息"));
    }

    @Test
    void getDisbursement_customerApplicationNotFound_shouldReturnError() throws Exception {
        when(loanApplicationMapper.selectById(3L)).thenReturn(null);

        mockMvc.perform(get("/api/disbursements/3")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("申请不存在"));
    }
}
