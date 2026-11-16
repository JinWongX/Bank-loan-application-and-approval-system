package com.bank.loan.service;

import com.bank.loan.dto.LoanProductDTO;

import java.util.List;

public interface LoanProductService {

    List<LoanProductDTO> listProducts(String status, boolean isAdmin);

    LoanProductDTO getProductById(Long id);

    Long createProduct(LoanProductDTO dto);

    void updateProduct(Long id, LoanProductDTO dto);

    void updateStatus(Long id, String status);

    void deleteProduct(Long id);
}
