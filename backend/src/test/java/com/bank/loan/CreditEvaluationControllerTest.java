package com.bank.loan;

import com.bank.loan.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CreditEvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtConfig jwtConfig;

    private String customerToken() {
        return jwtConfig.generateToken(1L, "customer1", "customer");
    }

    private String managerToken() {
        return jwtConfig.generateToken(2L, "manager1", "manager");
    }

    @Test
    void evaluate_customerForbidden() throws Exception {
        mockMvc.perform(post("/api/credit-evaluations/1")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void evaluate_managerSuccess() throws Exception {
        mockMvc.perform(post("/api/credit-evaluations/1")
                        .header("Authorization", "Bearer " + managerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.score").isNumber())
                .andExpect(jsonPath("$.data.grade").isString());
    }

    @Test
    void getEvaluation_customerOwnSuccess() throws Exception {
        // 先触发评估，确保有评估记录
        mockMvc.perform(post("/api/credit-evaluations/1")
                        .header("Authorization", "Bearer " + managerToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/credit-evaluations/1")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.score").isNumber());
    }
}
