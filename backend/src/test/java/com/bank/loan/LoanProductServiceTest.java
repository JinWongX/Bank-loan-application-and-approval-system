package com.bank.loan;

import com.bank.loan.dto.LoanProductDTO;
import com.bank.loan.entity.LoanProduct;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.LoanProductMapper;
import com.bank.loan.service.impl.LoanProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanProductServiceTest {

    @Mock
    private LoanProductMapper loanProductMapper;

    @Mock
    private LoanApplicationMapper loanApplicationMapper;

    @InjectMocks
    private LoanProductServiceImpl loanProductService;

    private LoanProductDTO buildValidDto() {
        LoanProductDTO dto = new LoanProductDTO();
        dto.setName("个人信用贷");
        dto.setType("信用贷款");
        dto.setMinAmount(new BigDecimal("5000"));
        dto.setMaxAmount(new BigDecimal("500000"));
        dto.setInterestRate(new BigDecimal("0.045"));
        dto.setMinTerm(6);
        dto.setMaxTerm(36);
        dto.setRepaymentMethod("等额本息");
        dto.setEligibilityConditions("年满18周岁，信用良好");
        dto.setStatus("ACTIVE");
        return dto;
    }

    @Test
    void createProduct_success() {
        when(loanProductMapper.insert(any(LoanProduct.class))).thenAnswer(invocation -> {
            LoanProduct p = invocation.getArgument(0);
            p.setId(1L);
            return 1;
        });

        Long id = loanProductService.createProduct(buildValidDto());

        assertEquals(1L, id);
        ArgumentCaptor<LoanProduct> captor = ArgumentCaptor.forClass(LoanProduct.class);
        verify(loanProductMapper).insert(captor.capture());
        LoanProduct inserted = captor.getValue();
        assertEquals("个人信用贷", inserted.getName());
        assertEquals(1, inserted.getStatus());
    }

    @Test
    void createProduct_amountRangeInvalid_shouldThrow() {
        LoanProductDTO dto = buildValidDto();
        dto.setMinAmount(new BigDecimal("100000"));
        dto.setMaxAmount(new BigDecimal("50000"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> loanProductService.createProduct(dto));
        assertEquals("最低额度不能大于最高额度", ex.getMessage());
        verify(loanProductMapper, never()).insert(any());
    }

    @Test
    void createProduct_termRangeInvalid_shouldThrow() {
        LoanProductDTO dto = buildValidDto();
        dto.setMinTerm(36);
        dto.setMaxTerm(6);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> loanProductService.createProduct(dto));
        assertEquals("最短期限不能大于最长期限", ex.getMessage());
    }

    @Test
    void createProduct_missingRequiredFields_shouldThrow() {
        LoanProductDTO dto = new LoanProductDTO();
        dto.setMinAmount(new BigDecimal("1000"));
        dto.setMaxAmount(new BigDecimal("10000"));

        assertThrows(RuntimeException.class, () -> loanProductService.createProduct(dto));
        verify(loanProductMapper, never()).insert(any());
    }

    @Test
    void updateProduct_success() {
        LoanProduct existing = new LoanProduct();
        existing.setId(1L);
        existing.setName("旧名称");
        existing.setStatus(1);

        when(loanProductMapper.selectById(1L)).thenReturn(existing);

        LoanProductDTO dto = buildValidDto();
        dto.setName("经营贷");
        dto.setStatus("INACTIVE");

        loanProductService.updateProduct(1L, dto);

        ArgumentCaptor<LoanProduct> captor = ArgumentCaptor.forClass(LoanProduct.class);
        verify(loanProductMapper).updateById(captor.capture());
        LoanProduct updated = captor.getValue();
        assertEquals(1L, updated.getId());
        assertEquals("经营贷", updated.getName());
        assertEquals(0, updated.getStatus());
    }

    @Test
    void updateProduct_notFound_shouldThrow() {
        when(loanProductMapper.selectById(99L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> loanProductService.updateProduct(99L, buildValidDto()));
    }

    @Test
    void updateStatus_deactivate() {
        LoanProduct existing = new LoanProduct();
        existing.setId(1L);
        existing.setStatus(1);
        when(loanProductMapper.selectById(1L)).thenReturn(existing);

        loanProductService.updateStatus(1L, "INACTIVE");

        ArgumentCaptor<LoanProduct> captor = ArgumentCaptor.forClass(LoanProduct.class);
        verify(loanProductMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
    }

    @Test
    void updateStatus_invalidStatus_shouldThrow() {
        LoanProduct existing = new LoanProduct();
        existing.setId(1L);
        existing.setStatus(1);
        when(loanProductMapper.selectById(1L)).thenReturn(existing);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanProductService.updateStatus(1L, "UNKNOWN"));
        assertTrue(ex.getMessage().contains("无效的产品状态"));
    }

    @Test
    void updateStatus_notFound_shouldThrow() {
        when(loanProductMapper.selectById(99L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> loanProductService.updateStatus(99L, "INACTIVE"));
    }

    @Test
    void listProducts_nonAdmin_shouldFilterActive() {
        LoanProduct product = new LoanProduct();
        product.setId(1L);
        product.setStatus(1);
        when(loanProductMapper.selectList(any())).thenReturn(List.of(product));

        List<LoanProductDTO> list = loanProductService.listProducts(null, false);
        assertEquals(1, list.size());
        assertEquals("ACTIVE", list.get(0).getStatus());
    }

    @Test
    void listProducts_adminWithStatus_shouldFilter() {
        LoanProduct product = new LoanProduct();
        product.setId(1L);
        product.setStatus(0);
        when(loanProductMapper.selectList(any())).thenReturn(List.of(product));

        List<LoanProductDTO> list = loanProductService.listProducts("INACTIVE", true);
        assertEquals(1, list.size());
        assertEquals("INACTIVE", list.get(0).getStatus());
    }

    @Test
    void listProducts_adminWithoutStatus_shouldReturnAll() {
        LoanProduct product = new LoanProduct();
        product.setId(1L);
        product.setStatus(1);
        when(loanProductMapper.selectList(any())).thenReturn(List.of(product));

        List<LoanProductDTO> list = loanProductService.listProducts("", true);
        assertEquals(1, list.size());
    }

    @Test
    void getProductById_notFound_shouldThrow() {
        when(loanProductMapper.selectById(99L)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanProductService.getProductById(99L));
        assertEquals("产品不存在", ex.getMessage());
    }

    @Test
    void getProductById_success() {
        LoanProduct product = new LoanProduct();
        product.setId(1L);
        product.setName("测试产品");
        product.setStatus(1);
        when(loanProductMapper.selectById(1L)).thenReturn(product);

        LoanProductDTO dto = loanProductService.getProductById(1L);
        assertEquals("测试产品", dto.getName());
        assertEquals("ACTIVE", dto.getStatus());
    }

    @Test
    void deleteProduct_success() {
        LoanProduct product = new LoanProduct();
        product.setId(1L);
        product.setName("测试产品");
        when(loanProductMapper.selectById(1L)).thenReturn(product);
        when(loanApplicationMapper.selectCount(any())).thenReturn(0L);

        loanProductService.deleteProduct(1L);

        verify(loanProductMapper, times(1)).deleteById(1L);
    }

    @Test
    void deleteProduct_withApplications_shouldThrow() {
        LoanProduct product = new LoanProduct();
        product.setId(1L);
        product.setName("测试产品");
        when(loanProductMapper.selectById(1L)).thenReturn(product);
        when(loanApplicationMapper.selectCount(any())).thenReturn(3L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanProductService.deleteProduct(1L));
        assertEquals("该产品已存在贷款申请，无法删除", ex.getMessage());
        verify(loanProductMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteProduct_notFound_shouldThrow() {
        when(loanProductMapper.selectById(99L)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loanProductService.deleteProduct(99L));
        assertEquals("产品不存在", ex.getMessage());
    }
}
