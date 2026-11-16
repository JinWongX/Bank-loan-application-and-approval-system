package com.bank.loan.service;

import com.bank.loan.dto.LoanApplicationDTO;
import com.bank.loan.dto.LoanDisbursementDTO;

import java.util.List;

public interface LoanDisbursementService {

    List<LoanApplicationDTO> listPendingApplications();

    LoanDisbursementDTO disburse(Long applicationId, LoanDisbursementDTO dto, Long operatorId);

    LoanDisbursementDTO getDisbursementByApplicationId(Long applicationId);
}
