package com.bank.loan;

import com.bank.loan.config.JwtConfig;
import com.bank.loan.dto.DueDiligenceDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DueDiligenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtConfig jwtConfig;

    private String customerToken() {
        return jwtConfig.generateToken(1L, "customer1", "customer");
    }

    private String managerToken() {
        return jwtConfig.generateToken(2L, "manager1", "manager");
    }

    private DueDiligenceDTO buildReportDto() {
        DueDiligenceDTO dto = new DueDiligenceDTO();
        dto.setBusinessSituation("经营正常");
        dto.setAssetsLiabilities("资产负债合理");
        dto.setRepaymentAbility("还款能力充足");
        dto.setConclusion("PASS");
        dto.setAttachmentPath("");
        return dto;
    }

    @Test
    void listPending_success() throws Exception {
        // 将申请 1 的状态变为 CREDIT_EVALUATED，使其出现在待办列表
        mockMvc.perform(post("/api/credit-evaluations/1")
                        .header("Authorization", "Bearer " + managerToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/due-diligence/pending")
                        .header("Authorization", "Bearer " + managerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[?(@.id==1)]").exists());
    }

    @Test
    void submitReport_success() throws Exception {
        // 确保申请 1 处于 CREDIT_EVALUATED 状态
        mockMvc.perform(post("/api/credit-evaluations/1")
                        .header("Authorization", "Bearer " + managerToken()))
                .andExpect(status().isOk());

        DueDiligenceDTO dto = buildReportDto();
        mockMvc.perform(post("/api/due-diligence/1")
                        .header("Authorization", "Bearer " + managerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.conclusion").value("PASS"))
                .andExpect(jsonPath("$.data.managerName").value("李经理"));
    }

    @Test
    void submitReport_customerForbidden() throws Exception {
        DueDiligenceDTO dto = buildReportDto();
        mockMvc.perform(post("/api/due-diligence/1")
                        .header("Authorization", "Bearer " + customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
