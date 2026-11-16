package com.bank.loan.service;

import com.bank.loan.dto.RepaymentScheduleDTO;

import java.util.List;

public interface RepaymentScheduleService {

    void generateSchedule(Long applicationId);

    List<RepaymentScheduleDTO> listByApplicationId(Long applicationId);
}
