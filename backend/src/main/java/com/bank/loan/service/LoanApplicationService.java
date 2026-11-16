package com.bank.loan.service;

import com.bank.loan.dto.ApplicationMaterialDTO;
import com.bank.loan.dto.LoanApplicationDTO;
import com.bank.loan.dto.LoanApplicationDetailDTO;

import java.util.List;

public interface LoanApplicationService {

    LoanApplicationDTO submitApplication(Long customerId, LoanApplicationDTO dto);

    List<LoanApplicationDTO> listMyApplications(Long customerId);

    LoanApplicationDTO getApplicationDetail(Long applicationId, Long customerId);

    List<LoanApplicationDTO> listApplications(String status);

    void addMaterials(Long applicationId, Long customerId, List<ApplicationMaterialDTO> materials);

    List<LoanApplicationDTO> listAssignedApplications(Long managerId);

    LoanApplicationDetailDTO getApplicationFullDetail(Long applicationId);
}
