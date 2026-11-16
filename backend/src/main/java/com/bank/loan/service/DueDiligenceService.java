package com.bank.loan.service;

import com.bank.loan.dto.DueDiligenceDTO;
import com.bank.loan.dto.LoanApplicationDTO;

import java.util.List;

public interface DueDiligenceService {

    List<LoanApplicationDTO> listPendingApplications(Long managerId);

    DueDiligenceDTO submitReport(Long applicationId, Long managerId, DueDiligenceDTO dto);

    DueDiligenceDTO getReport(Long applicationId);
}
