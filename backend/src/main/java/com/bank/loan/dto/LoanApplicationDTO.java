package com.bank.loan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LoanApplicationDTO {

    private Long id;

    private String applicationNo;

    private Long customerId;

    private Long productId;

    private BigDecimal amount;

    private Integer termMonths;

    private String purpose;

    private String repaymentSource;

    private String status;

    private Long managerId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private LoanProductDTO product;

    private List<ApplicationMaterialDTO> materials;

    private List<ApplicationMaterialDTO> newMaterials;

    private String customerName;

    private String creditGrade;
}
