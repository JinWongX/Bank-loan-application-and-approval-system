package com.bank.loan.service.impl;

import com.bank.loan.dto.CreditEvaluationDTO;
import com.bank.loan.entity.CreditEvaluation;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.SysUser;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.mapper.CreditEvaluationMapper;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.SysUserMapper;
import com.bank.loan.service.CreditEvaluationService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class CreditEvaluationServiceImpl implements CreditEvaluationService {

    @Autowired
    private LoanApplicationMapper loanApplicationMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private CreditEvaluationMapper creditEvaluationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditEvaluationDTO evaluate(Long applicationId) {
        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }

        SysUser user = sysUserMapper.selectById(application.getCustomerId());
        if (user == null) {
            throw new RuntimeException("申请人信息不存在");
        }

        int score = 0;

        // 年龄
        Integer age = user.getAge();
        if (age != null && age >= 25 && age <= 45) {
            score += 25;
        } else if (age != null && ((age >= 18 && age <= 24) || (age >= 46 && age <= 55))) {
            score += 15;
        } else {
            score += 5;
        }

        // 月收入
        BigDecimal income = user.getMonthlyIncome();
        if (income != null && income.compareTo(new BigDecimal("30000")) >= 0) {
            score += 30;
        } else if (income != null && income.compareTo(new BigDecimal("10000")) >= 0) {
            score += 20;
        } else if (income != null && income.compareTo(new BigDecimal("5000")) >= 0) {
            score += 10;
        } else {
            score += 5;
        }

        // 征信记录
        String creditStatus = user.getCreditStatus();
        if (creditStatus != null) {
            String cs = creditStatus.trim();
            if ("GOOD".equalsIgnoreCase(cs) || "优秀".equals(cs) || "良好".equals(cs)) {
                score += 25;
            } else if ("FAIR".equalsIgnoreCase(cs) || "一般".equals(cs)) {
                score += 15;
            } else {
                score += 5;
            }
        } else {
            score += 5;
        }

        // 负债率
        BigDecimal debt = user.getMonthlyDebt();
        BigDecimal ratio = null;
        if (income != null && income.compareTo(BigDecimal.ZERO) > 0 && debt != null) {
            ratio = debt.divide(income, 4, RoundingMode.HALF_UP);
            double r = ratio.doubleValue();
            if (r < 0.3) {
                score += 20;
            } else if (r <= 0.5) {
                score += 10;
            }
        }

        // 等级
        String grade;
        if (score >= 80) {
            grade = "A";
        } else if (score >= 60) {
            grade = "B";
        } else if (score >= 40) {
            grade = "C";
        } else {
            grade = "D";
        }

        // 建议
        String suggestion;
        switch (grade) {
            case "A":
                suggestion = "信用优秀，建议放款";
                break;
            case "B":
                suggestion = "信用良好，建议正常审批";
                break;
            case "C":
                suggestion = "信用一般，建议补充材料或担保后审批";
                break;
            default:
                suggestion = "信用较差，建议拒绝";
                break;
        }
        if (ratio != null && ratio.doubleValue() > 0.5) {
            suggestion += "；负债率偏高，建议谨慎";
        }

        // 保存评估结果
        QueryWrapper<CreditEvaluation> wrapper = new QueryWrapper<>();
        wrapper.eq("application_id", applicationId);
        CreditEvaluation evaluation = creditEvaluationMapper.selectOne(wrapper);
        if (evaluation == null) {
            evaluation = new CreditEvaluation();
        }
        evaluation.setApplicationId(applicationId);
        evaluation.setScore(score);
        evaluation.setGrade(grade);
        evaluation.setEvaluationSuggestion(suggestion);
        evaluation.setEvaluateTime(LocalDateTime.now());
        if (evaluation.getId() == null) {
            creditEvaluationMapper.insert(evaluation);
        } else {
            creditEvaluationMapper.updateById(evaluation);
        }

        // 更新申请状态
        application.setStatus(ApplicationStatus.CREDIT_EVALUATED.name());
        application.setUpdateTime(LocalDateTime.now());
        loanApplicationMapper.updateById(application);

        return toDto(evaluation);
    }

    @Override
    public CreditEvaluationDTO getEvaluation(Long applicationId, Long customerId) {
        if (customerId != null) {
            LoanApplication application = loanApplicationMapper.selectById(applicationId);
            if (application == null || !customerId.equals(application.getCustomerId())) {
                throw new AccessDeniedException("无权查看该信用评估");
            }
        }
        QueryWrapper<CreditEvaluation> wrapper = new QueryWrapper<>();
        wrapper.eq("application_id", applicationId);
        CreditEvaluation evaluation = creditEvaluationMapper.selectOne(wrapper);
        if (evaluation == null) {
            return null;
        }
        return toDto(evaluation);
    }

    private CreditEvaluationDTO toDto(CreditEvaluation evaluation) {
        CreditEvaluationDTO dto = new CreditEvaluationDTO();
        BeanUtils.copyProperties(evaluation, dto);
        return dto;
    }
}
