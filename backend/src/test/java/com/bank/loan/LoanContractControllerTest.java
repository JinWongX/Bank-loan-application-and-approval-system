package com.bank.loan;

import com.bank.loan.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoanContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtConfig jwtConfig;

    private String customerToken() {
        return jwtConfig.generateToken(1L, "customer1", "customer");
    }

    private String adminToken() {
        return jwtConfig.generateToken(7L, "admin", "admin");
    }

    @Test
    void generateContract_success() throws Exception {
        mockMvc.perform(post("/api/contracts/generate/1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.signStatus").value("UNSIGNED"))
                .andExpect(jsonPath("$.data.contractContent").value(containsString("贷款金额（大写）")));
    }

    @Test
    void generateContract_customerForbidden() throws Exception {
        mockMvc.perform(post("/api/contracts/generate/1")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void getContract_success() throws Exception {
        // 先为申请 1 生成合同
        mockMvc.perform(post("/api/contracts/generate/1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/contracts/1")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.applicationId").value(1))
                .andExpect(jsonPath("$.data.signStatus").value("UNSIGNED"));
    }

    @Test
    void signContract_success() throws Exception {
        // 使用测试数据中已经存在且未签署的合同 ID 1（对应申请 2）
        mockMvc.perform(post("/api/contracts/sign/1")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.signStatus").value("SIGNED"));
    }

    @Test
    void listMyContracts_success() throws Exception {
        // 为申请 1 生成合同
        mockMvc.perform(post("/api/contracts/generate/1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/contracts/my/list")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
