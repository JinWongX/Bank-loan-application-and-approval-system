package com.bank.loan;

import com.bank.loan.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtConfig jwtConfig;

    private String tokenFor(String role) {
        return jwtConfig.generateToken(1L, "user1", role);
    }

    @Test
    void getMenu_customer_shouldReturnCustomerMenus() throws Exception {
        mockMvc.perform(get("/api/menu")
                        .header("Authorization", "Bearer " + tokenFor("customer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("home"))
                .andExpect(jsonPath("$.data[?(@.name=='apply')]").exists());
    }

    @Test
    void getMenu_manager_shouldReturnManagerMenus() throws Exception {
        mockMvc.perform(get("/api/menu")
                        .header("Authorization", "Bearer " + tokenFor("manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[?(@.name=='pending')]").exists());
    }

    @Test
    void getMenu_approver_shouldReturnApproverMenus() throws Exception {
        mockMvc.perform(get("/api/menu")
                        .header("Authorization", "Bearer " + tokenFor("approver_level1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[?(@.name=='pending')]").exists());
    }

    @Test
    void getMenu_disbursementOfficer_shouldReturnDisburserMenus() throws Exception {
        mockMvc.perform(get("/api/menu")
                        .header("Authorization", "Bearer " + tokenFor("disbursement_officer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[?(@.name=='pending')]").exists());
    }

    @Test
    void getMenu_admin_shouldReturnAdminMenus() throws Exception {
        mockMvc.perform(get("/api/menu")
                        .header("Authorization", "Bearer " + tokenFor("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[?(@.name=='dashboard')]").exists());
    }

    @Test
    void getMenu_noAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/menu"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminOnly_admin_shouldSuccess() throws Exception {
        mockMvc.perform(get("/api/admin-only")
                        .header("Authorization", "Bearer " + tokenFor("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("admin only"));
    }

    @Test
    void adminOnly_customer_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/admin-only")
                        .header("Authorization", "Bearer " + tokenFor("customer")))
                .andExpect(status().isForbidden());
    }
}
