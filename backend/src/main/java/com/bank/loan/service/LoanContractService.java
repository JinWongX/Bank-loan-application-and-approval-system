package com.bank.loan.service;

import com.bank.loan.dto.LoanContractDTO;

import java.util.List;

public interface LoanContractService {

    LoanContractDTO generateContract(Long applicationId);

    LoanContractDTO getContractByApplicationId(Long applicationId, Long customerId);

    LoanContractDTO signContract(Long contractId, Long customerId);

    List<LoanContractDTO> listMyContracts(Long customerId);
}
