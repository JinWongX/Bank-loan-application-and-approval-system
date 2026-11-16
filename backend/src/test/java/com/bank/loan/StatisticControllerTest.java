package com.bank.loan;

import com.bank.loan.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StatisticControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtConfig jwtConfig;

    private String adminToken() {
        return jwtConfig.generateToken(7L, "admin", "admin");
    }

    private String customerToken() {
        return jwtConfig.generateToken(1L, "customer1", "customer");
    }

    @Test
    void dashboard_success() throws Exception {
        mockMvc.perform(get("/api/statistics/dashboard")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pendingApplicationCount").value(3))
                .andExpect(jsonPath("$.data.monthApplicationCount").value(5))
                .andExpect(jsonPath("$.data.monthDisbursementAmount").value(300000.0));
    }

    @Test
    void byProduct_success() throws Exception {
        mockMvc.perform(get("/api/statistics/by-product")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].productName").isString());
    }

    @Test
    void byManager_success() throws Exception {
        mockMvc.perform(get("/api/statistics/by-manager")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].managerName").isString());
    }

    @Test
    void byTime_success() throws Exception {
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DateTimeFormatter.ISO_DATE);
        mockMvc.perform(get("/api/statistics/by-time")
                        .param("startDate", todayStr)
                        .param("endDate", todayStr)
                        .param("groupBy", "DAY")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].period").value(todayStr));
    }

    @Test
    void exportExcel_success() throws Exception {
        mockMvc.perform(get("/api/statistics/export")
                        .param("type", "excel")
                        .param("dimension", "product")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", containsString("product_statistics.xlsx")));
    }

    @Test
    void exportCsv_success() throws Exception {
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DateTimeFormatter.ISO_DATE);
        mockMvc.perform(get("/api/statistics/export")
                        .param("type", "csv")
                        .param("dimension", "time")
                        .param("startDate", todayStr)
                        .param("endDate", todayStr)
                        .param("groupBy", "DAY")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("time_statistics.csv")))
                .andExpect(content().string(containsString("时间段")));
    }

    @Test
    void dashboard_customerForbidden() throws Exception {
        mockMvc.perform(get("/api/statistics/dashboard")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
