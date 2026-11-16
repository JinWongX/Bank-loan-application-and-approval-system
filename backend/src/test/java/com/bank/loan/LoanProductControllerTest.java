package com.bank.loan;

import com.bank.loan.dto.LoanProductDTO;
import com.bank.loan.service.LoanProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoanProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanProductService loanProductService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_shouldSucceed() throws Exception {
        LoanProductDTO dto = new LoanProductDTO();
        dto.setName("测试产品");
        dto.setType("信用贷");
        dto.setMinAmount(new BigDecimal("1000"));
        dto.setMaxAmount(new BigDecimal("50000"));
        dto.setInterestRate(new BigDecimal("0.05"));
        dto.setMinTerm(1);
        dto.setMaxTerm(12);
        dto.setRepaymentMethod("等额本息");
        dto.setStatus("ACTIVE");

        when(loanProductService.createProduct(any())).thenReturn(1L);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_shouldSucceed() throws Exception {
        doNothing().when(loanProductService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(loanProductService, times(1)).deleteProduct(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStatus_shouldSucceed() throws Exception {
        doNothing().when(loanProductService).updateStatus(eq(1L), anyString());

        Map<String, String> body = new HashMap<>();
        body.put("status", "INACTIVE");

        mockMvc.perform(patch("/api/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createProduct_withoutAdminRole_shouldReturn403() throws Exception {
        LoanProductDTO dto = new LoanProductDTO();
        dto.setName("测试产品");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }
}
