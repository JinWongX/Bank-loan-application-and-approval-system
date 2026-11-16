package com.bank.loan.controller;

import com.bank.loan.common.Result;
import com.bank.loan.dto.LoanProductDTO;
import com.bank.loan.service.LoanProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class LoanProductController {

    @Autowired
    private LoanProductService loanProductService;

    @GetMapping
    public Result<List<LoanProductDTO>> listProducts(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        boolean isAdmin = isAdmin(authentication);
        List<LoanProductDTO> list = loanProductService.listProducts(status, isAdmin);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<LoanProductDTO> getProduct(@PathVariable Long id) {
        LoanProductDTO dto = loanProductService.getProductById(id);
        return Result.success(dto);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> createProduct(@RequestBody LoanProductDTO dto) {
        Long id = loanProductService.createProduct(dto);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody LoanProductDTO dto) {
        loanProductService.updateProduct(id, dto);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        loanProductService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        loanProductService.deleteProduct(id);
        return Result.success();
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
