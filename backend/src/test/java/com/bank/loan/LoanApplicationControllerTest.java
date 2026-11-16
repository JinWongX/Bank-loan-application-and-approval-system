package com.bank.loan;

import com.bank.loan.config.JwtConfig;
import com.bank.loan.dto.ApplicationMaterialDTO;
import com.bank.loan.dto.LoanApplicationDTO;
import com.bank.loan.service.LoanApplicationService;
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
import java.util.Collections;
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
class LoanApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtConfig jwtConfig;

    @MockBean
    private LoanApplicationService loanApplicationService;

    private String customerToken() {
        return jwtConfig.generateToken(1L, "customer1", "customer");
    }

    private ApplicationMaterialDTO createMaterial(String type, String fileName, String filePath) {
        ApplicationMaterialDTO dto = new ApplicationMaterialDTO();
        dto.setMaterialType(type);
        dto.setFileName(fileName);
        dto.setFilePath(filePath);
        return dto;
    }

    @Test
    void listMyApplications_success() throws Exception {
        LoanApplicationDTO dto = new LoanApplicationDTO();
        dto.setId(1L);
        dto.setApplicationNo("LA202609010001");
        dto.setAmount(new BigDecimal("100000"));
        dto.setTermMonths(12);

        when(loanApplicationService.listMyApplications(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/applications/my")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].applicationNo").value("LA202609010001"));
    }

    @Test
    void listApplications_withoutPermission_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void submitApplication_success() throws Exception {
        LoanApplicationDTO result = new LoanApplicationDTO();
        result.setId(1L);
        result.setApplicationNo("LA202609010002");
        result.setAmount(new BigDecimal("80000"));
        result.setTermMonths(12);

        when(loanApplicationService.submitApplication(eq(1L), any(LoanApplicationDTO.class)))
                .thenReturn(result);

        LoanApplicationDTO request = new LoanApplicationDTO();
        request.setProductId(1L);
        request.setAmount(new BigDecimal("80000"));
        request.setTermMonths(12);
        request.setPurpose("装修");
        request.setRepaymentSource("工资收入");
        request.setNewMaterials(List.of(
                createMaterial("ID_CARD_FRONT", "id_front.jpg", "http://localhost:8080/api/download/aaaabbbbccccddddeeeeffff00001111.jpg"),
                createMaterial("ID_CARD_BACK", "id_back.jpg", "http://localhost:8080/api/download/aaaabbbbccccddddeeeeffff00001112.jpg"),
                createMaterial("INCOME_PROOF", "income.pdf", "http://localhost:8080/api/download/aaaabbbbccccddddeeeeffff00001113.pdf"),
                createMaterial("BANK_STATEMENT", "bank.pdf", "http://localhost:8080/api/download/aaaabbbbccccddddeeeeffff00001114.pdf")
        ));

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.applicationNo").value("LA202609010002"))
                .andExpect(jsonPath("$.data.amount").value(80000));
    }
}
