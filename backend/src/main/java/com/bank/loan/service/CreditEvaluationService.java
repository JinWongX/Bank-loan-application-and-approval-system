package com.bank.loan.service;

import com.bank.loan.dto.CreditEvaluationDTO;

public interface CreditEvaluationService {

    CreditEvaluationDTO evaluate(Long applicationId);

    CreditEvaluationDTO getEvaluation(Long applicationId, Long customerId);
}
