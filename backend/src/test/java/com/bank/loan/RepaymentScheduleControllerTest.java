package com.bank.loan;

import com.bank.loan.config.JwtConfig;
import com.bank.loan.dto.RepaymentScheduleDTO;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.service.RepaymentScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RepaymentScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtConfig jwtConfig;

    @MockBean
    private RepaymentScheduleService repaymentScheduleService;

    @MockBean
    private LoanApplicationMapper loanApplicationMapper;

    private String customerToken() {
        return jwtConfig.generateToken(1L, "customer1", "customer");
    }

    private String officerToken() {
        return jwtConfig.generateToken(6L, "officer1", "disbursement_officer");
    }

    @Test
    void listSchedules_customerOwn_shouldSuccess() throws Exception {
        LoanApplication application = new LoanApplication();
        application.setId(3L);
        application.setCustomerId(1L);
        when(loanApplicationMapper.selectById(3L)).thenReturn(application);

        RepaymentScheduleDTO dto = new RepaymentScheduleDTO();
        dto.setId(1L);
        dto.setPeriod(1);
        dto.setPrincipal(new BigDecimal("10000"));
        dto.setDueDate(LocalDate.now());
        when(repaymentScheduleService.listByApplicationId(3L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/repayment-schedules/3")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].period").value(1));
    }

    @Test
    void listSchedules_customerNotOwn_shouldReturnError() throws Exception {
        LoanApplication application = new LoanApplication();
        application.setId(3L);
        application.setCustomerId(2L);
        when(loanApplicationMapper.selectById(3L)).thenReturn(application);

        mockMvc.perform(get("/api/repayment-schedules/3")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("无权查看该还款计划"));
    }

    @Test
    void listSchedules_customerApplicationNotFound_shouldReturnError() throws Exception {
        when(loanApplicationMapper.selectById(3L)).thenReturn(null);

        mockMvc.perform(get("/api/repayment-schedules/3")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("申请不存在"));
    }

    @Test
    void listSchedules_officer_shouldSuccess() throws Exception {
        RepaymentScheduleDTO dto = new RepaymentScheduleDTO();
        dto.setId(1L);
        dto.setPeriod(1);
        when(repaymentScheduleService.listByApplicationId(3L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/repayment-schedules/3")
                        .header("Authorization", "Bearer " + officerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].period").value(1));
    }
}
