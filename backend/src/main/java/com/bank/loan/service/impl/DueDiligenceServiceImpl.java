package com.bank.loan.service.impl;

import com.bank.loan.dto.DueDiligenceDTO;
import com.bank.loan.dto.LoanApplicationDTO;
import com.bank.loan.dto.LoanProductDTO;
import com.bank.loan.entity.CreditEvaluation;
import com.bank.loan.entity.DueDiligence;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.SysUser;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.mapper.CreditEvaluationMapper;
import com.bank.loan.mapper.DueDiligenceMapper;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.SysUserMapper;
import com.bank.loan.service.DueDiligenceService;
import com.bank.loan.service.LoanProductService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DueDiligenceServiceImpl implements DueDiligenceService {

    @Autowired
    private LoanApplicationMapper loanApplicationMapper;

    @Autowired
    private DueDiligenceMapper dueDiligenceMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private CreditEvaluationMapper creditEvaluationMapper;

    @Autowired
    private LoanProductService loanProductService;

    @Override
    public List<LoanApplicationDTO> listPendingApplications(Long managerId) {
        QueryWrapper<LoanApplication> wrapper = new QueryWrapper<>();
        wrapper.eq("status", ApplicationStatus.CREDIT_EVALUATED.name())
                .and(w -> w.isNull("manager_id").or().eq("manager_id", managerId))
                .orderByDesc("create_time");
        List<LoanApplication> list = loanApplicationMapper.selectList(wrapper);
        return list.stream().map(this::toPendingDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DueDiligenceDTO submitReport(Long applicationId, Long managerId, DueDiligenceDTO dto) {
        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!ApplicationStatus.CREDIT_EVALUATED.name().equals(application.getStatus())) {
            throw new RuntimeException("当前申请状态不允许提交尽调报告");
        }

        String conclusion = dto.getConclusion();
        if (!StringUtils.hasText(conclusion)) {
            throw new RuntimeException("尽调结论不能为空");
        }
        if (!isValidConclusion(conclusion)) {
            throw new RuntimeException("尽调结论无效");
        }

        // 保存或更新尽调报告
        QueryWrapper<DueDiligence> ddWrapper = new QueryWrapper<>();
        ddWrapper.eq("application_id", applicationId);
        DueDiligence dueDiligence = dueDiligenceMapper.selectOne(ddWrapper);
        if (dueDiligence == null) {
            dueDiligence = new DueDiligence();
        }
        dueDiligence.setApplicationId(applicationId);
        dueDiligence.setManagerId(managerId);
        dueDiligence.setBusinessSituation(dto.getBusinessSituation());
        dueDiligence.setAssetsLiabilities(dto.getAssetsLiabilities());
        dueDiligence.setRepaymentAbility(dto.getRepaymentAbility());
        dueDiligence.setConclusion(conclusion);
        dueDiligence.setAttachmentPath(dto.getAttachmentPath());
        dueDiligence.setSubmitTime(LocalDateTime.now());
        if (dueDiligence.getId() == null) {
            dueDiligenceMapper.insert(dueDiligence);
        } else {
            dueDiligenceMapper.updateById(dueDiligence);
        }

        // 更新申请状态及客户经理
        application.setManagerId(managerId);
        if ("SUPPLEMENT".equals(conclusion)) {
            application.setStatus(ApplicationStatus.MATERIAL_SUPPLEMENT.name());
        } else {
            application.setStatus(ApplicationStatus.DUE_DILIGENCE_COMPLETED.name());
        }
        application.setUpdateTime(LocalDateTime.now());
        loanApplicationMapper.updateById(application);

        return toDto(dueDiligence);
    }

    @Override
    public DueDiligenceDTO getReport(Long applicationId) {
        QueryWrapper<DueDiligence> wrapper = new QueryWrapper<>();
        wrapper.eq("application_id", applicationId);
        DueDiligence dueDiligence = dueDiligenceMapper.selectOne(wrapper);
        if (dueDiligence == null) {
            return null;
        }
        return toDto(dueDiligence);
    }

    private boolean isValidConclusion(String conclusion) {
        return "PASS".equals(conclusion) || "SUGGEST_REJECT".equals(conclusion) || "SUPPLEMENT".equals(conclusion);
    }

    private LoanApplicationDTO toPendingDto(LoanApplication application) {
        LoanApplicationDTO dto = new LoanApplicationDTO();
        BeanUtils.copyProperties(application, dto);
        dto.setProduct(toProductDto(application.getProductId()));

        SysUser customer = sysUserMapper.selectById(application.getCustomerId());
        if (customer != null) {
            dto.setCustomerName(customer.getRealName());
        }

        QueryWrapper<CreditEvaluation> wrapper = new QueryWrapper<>();
        wrapper.eq("application_id", application.getId());
        CreditEvaluation evaluation = creditEvaluationMapper.selectOne(wrapper);
        if (evaluation != null) {
            dto.setCreditGrade(evaluation.getGrade());
        }
        return dto;
    }

    private LoanProductDTO toProductDto(Long productId) {
        if (productId == null) {
            return null;
        }
        try {
            return loanProductService.getProductById(productId);
        } catch (Exception e) {
            return null;
        }
    }

    private DueDiligenceDTO toDto(DueDiligence dueDiligence) {
        DueDiligenceDTO dto = new DueDiligenceDTO();
        BeanUtils.copyProperties(dueDiligence, dto);
        if (dueDiligence.getManagerId() != null) {
            SysUser manager = sysUserMapper.selectById(dueDiligence.getManagerId());
            if (manager != null) {
                dto.setManagerName(manager.getRealName());
            }
        }
        return dto;
    }
}
