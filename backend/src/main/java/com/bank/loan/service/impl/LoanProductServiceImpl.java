package com.bank.loan.service.impl;

import com.bank.loan.dto.LoanProductDTO;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.LoanProduct;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.LoanProductMapper;
import com.bank.loan.service.LoanProductService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanProductServiceImpl implements LoanProductService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    @Autowired
    private LoanProductMapper loanProductMapper;

    @Autowired
    private LoanApplicationMapper loanApplicationMapper;

    @Override
    public List<LoanProductDTO> listProducts(String status, boolean isAdmin) {
        QueryWrapper<LoanProduct> wrapper = new QueryWrapper<>();
        if (!isAdmin) {
            wrapper.eq("status", toDbStatus(STATUS_ACTIVE));
        } else if (StringUtils.hasText(status)) {
            wrapper.eq("status", toDbStatus(status));
        }
        wrapper.orderByDesc("create_time");
        List<LoanProduct> list = loanProductMapper.selectList(wrapper);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public LoanProductDTO getProductById(Long id) {
        LoanProduct product = loanProductMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("产品不存在");
        }
        return toDto(product);
    }

    @Override
    public Long createProduct(LoanProductDTO dto) {
        validateProduct(dto);
        LoanProduct product = toEntity(dto);
        product.setStatus(toDbStatus(dto.getStatus()));
        loanProductMapper.insert(product);
        return product.getId();
    }

    @Override
    public void updateProduct(Long id, LoanProductDTO dto) {
        LoanProduct existing = loanProductMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("产品不存在");
        }
        validateProduct(dto);
        LoanProduct product = toEntity(dto);
        product.setId(id);
        if (StringUtils.hasText(dto.getStatus())) {
            product.setStatus(toDbStatus(dto.getStatus()));
        } else {
            product.setStatus(existing.getStatus());
        }
        loanProductMapper.updateById(product);
    }

    @Override
    public void updateStatus(Long id, String status) {
        LoanProduct existing = loanProductMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("产品不存在");
        }
        Integer dbStatus = toDbStatus(status);
        LoanProduct product = new LoanProduct();
        product.setId(id);
        product.setStatus(dbStatus);
        loanProductMapper.updateById(product);
    }

    @Override
    public void deleteProduct(Long id) {
        LoanProduct existing = loanProductMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("产品不存在");
        }
        QueryWrapper<LoanApplication> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", id);
        long count = loanApplicationMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该产品已存在贷款申请，无法删除");
        }
        loanProductMapper.deleteById(id);
    }

    private void validateProduct(LoanProductDTO dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("产品名称不能为空");
        }
        if (dto.getMinAmount() == null || dto.getMaxAmount() == null) {
            throw new RuntimeException("额度范围不能为空");
        }
        if (dto.getMinAmount().compareTo(dto.getMaxAmount()) > 0) {
            throw new RuntimeException("最低额度不能大于最高额度");
        }
        if (dto.getMinTerm() == null || dto.getMaxTerm() == null) {
            throw new RuntimeException("期限范围不能为空");
        }
        if (dto.getMinTerm() > dto.getMaxTerm()) {
            throw new RuntimeException("最短期限不能大于最长期限");
        }
        if (dto.getInterestRate() == null) {
            throw new RuntimeException("利率不能为空");
        }
        if (!StringUtils.hasText(dto.getRepaymentMethod())) {
            throw new RuntimeException("还款方式不能为空");
        }
    }

    private LoanProductDTO toDto(LoanProduct product) {
        LoanProductDTO dto = new LoanProductDTO();
        BeanUtils.copyProperties(product, dto);
        dto.setStatus(toStatusString(product.getStatus()));
        return dto;
    }

    private LoanProduct toEntity(LoanProductDTO dto) {
        LoanProduct product = new LoanProduct();
        BeanUtils.copyProperties(dto, product);
        product.setId(null);
        return product;
    }

    private Integer toDbStatus(String status) {
        if (STATUS_ACTIVE.equalsIgnoreCase(status)) {
            return 1;
        }
        if (STATUS_INACTIVE.equalsIgnoreCase(status)) {
            return 0;
        }
        throw new RuntimeException("无效的产品状态：" + status);
    }

    private String toStatusString(Integer status) {
        if (status == null) {
            return STATUS_INACTIVE;
        }
        return status == 1 ? STATUS_ACTIVE : STATUS_INACTIVE;
    }
}
