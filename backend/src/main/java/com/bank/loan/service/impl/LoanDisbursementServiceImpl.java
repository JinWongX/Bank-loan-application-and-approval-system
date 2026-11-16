package com.bank.loan.service.impl;

import com.bank.loan.dto.LoanApplicationDTO;
import com.bank.loan.dto.LoanDisbursementDTO;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.LoanDisbursement;
import com.bank.loan.entity.SysUser;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.LoanDisbursementMapper;
import com.bank.loan.mapper.SysUserMapper;
import com.bank.loan.service.LoanApplicationService;
import com.bank.loan.service.LoanDisbursementService;
import com.bank.loan.service.RepaymentScheduleService;
import com.bank.loan.util.AesUtil;
import com.bank.loan.util.SensitiveDataUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanDisbursementServiceImpl implements LoanDisbursementService {

    @Autowired
    private LoanApplicationMapper loanApplicationMapper;

    @Autowired
    private LoanDisbursementMapper loanDisbursementMapper;

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Autowired
    private RepaymentScheduleService repaymentScheduleService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AesUtil aesUtil;

    @Override
    public List<LoanApplicationDTO> listPendingApplications() {
        List<LoanApplicationDTO> list = loanApplicationService.listApplications(ApplicationStatus.SIGNED.name());
        for (LoanApplicationDTO dto : list) {
            if (dto.getCustomerId() != null) {
                SysUser user = sysUserMapper.selectById(dto.getCustomerId());
                if (user != null) {
                    dto.setCustomerName(user.getRealName());
                }
            }
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoanDisbursementDTO disburse(Long applicationId, LoanDisbursementDTO dto, Long operatorId) {
        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!ApplicationStatus.SIGNED.name().equals(application.getStatus())) {
            throw new RuntimeException("当前申请状态不允许放款");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("放款金额必须大于0");
        }
        if (application.getAmount() == null || dto.getAmount().compareTo(application.getAmount()) != 0) {
            throw new RuntimeException("放款金额必须等于申请金额：" + application.getAmount());
        }

        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setApplicationId(applicationId);
        disbursement.setDisbursementTime(dto.getDisbursementTime() == null ? LocalDateTime.now() : dto.getDisbursementTime());
        disbursement.setAmount(dto.getAmount());
        disbursement.setAccountNo(aesUtil.encrypt(dto.getAccountNo()));
        disbursement.setBankName(dto.getBankName());
        disbursement.setOperatorId(operatorId);
        disbursement.setCreateTime(LocalDateTime.now());
        loanDisbursementMapper.insert(disbursement);

        application.setStatus(ApplicationStatus.DISBURSED.name());
        application.setUpdateTime(LocalDateTime.now());
        loanApplicationMapper.updateById(application);

        repaymentScheduleService.generateSchedule(applicationId);

        return toDto(disbursement);
    }

    @Override
    public LoanDisbursementDTO getDisbursementByApplicationId(Long applicationId) {
        QueryWrapper<LoanDisbursement> wrapper = new QueryWrapper<>();
        wrapper.eq("application_id", applicationId);
        LoanDisbursement disbursement = loanDisbursementMapper.selectOne(wrapper);
        if (disbursement == null) {
            return null;
        }
        return toDto(disbursement);
    }

    private LoanDisbursementDTO toDto(LoanDisbursement disbursement) {
        LoanDisbursementDTO dto = new LoanDisbursementDTO();
        BeanUtils.copyProperties(disbursement, dto);
        dto.setAccountNo(SensitiveDataUtil.maskBankAccount(aesUtil.decrypt(disbursement.getAccountNo())));
        return dto;
    }
}
