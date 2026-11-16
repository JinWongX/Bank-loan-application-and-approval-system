package com.bank.loan.service.impl;

import com.bank.loan.dto.ApplicationMaterialDTO;
import com.bank.loan.dto.ApprovalRecordDTO;
import com.bank.loan.dto.CreditEvaluationDTO;
import com.bank.loan.dto.DueDiligenceDTO;
import com.bank.loan.dto.LoanApplicationDTO;
import com.bank.loan.dto.LoanApplicationDetailDTO;
import com.bank.loan.dto.LoanProductDTO;
import com.bank.loan.entity.ApplicationMaterial;
import com.bank.loan.entity.ApprovalRecord;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.LoanProduct;
import com.bank.loan.entity.SysUser;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.mapper.ApplicationMaterialMapper;
import com.bank.loan.mapper.ApprovalRecordMapper;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.LoanProductMapper;
import com.bank.loan.mapper.SysUserMapper;
import com.bank.loan.service.CreditEvaluationService;
import com.bank.loan.service.DueDiligenceService;
import com.bank.loan.service.LoanApplicationService;
import com.bank.loan.service.LoanProductService;
import com.bank.loan.service.SysUserService;
import com.bank.loan.util.SensitiveDataUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LoanApplicationServiceImpl implements LoanApplicationService {

    @Autowired
    private LoanApplicationMapper loanApplicationMapper;

    @Autowired
    private ApplicationMaterialMapper applicationMaterialMapper;

    @Autowired
    private LoanProductMapper loanProductMapper;

    @Autowired
    private LoanProductService loanProductService;

    @Autowired
    private CreditEvaluationService creditEvaluationService;

    @Autowired
    private DueDiligenceService dueDiligenceService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    private static final Set<String> REQUIRED_MATERIAL_TYPES = new HashSet<>(Arrays.asList(
            "ID_CARD_FRONT", "ID_CARD_BACK", "INCOME_PROOF", "BANK_STATEMENT"
    ));

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoanApplicationDTO submitApplication(Long customerId, LoanApplicationDTO dto) {
        LoanProduct product = validateProductAndParams(dto.getProductId(), dto.getAmount(), dto.getTermMonths());
        validateMaterials(dto.getNewMaterials());

        LoanApplication application = new LoanApplication();
        application.setApplicationNo(generateApplicationNo());
        application.setCustomerId(customerId);
        application.setProductId(product.getId());
        application.setAmount(dto.getAmount());
        application.setTermMonths(dto.getTermMonths());
        application.setPurpose(dto.getPurpose());
        application.setRepaymentSource(dto.getRepaymentSource());
        application.setStatus(ApplicationStatus.SUBMITTED.name());
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        loanApplicationMapper.insert(application);

        if (!CollectionUtils.isEmpty(dto.getNewMaterials())) {
            saveMaterials(application.getId(), dto.getNewMaterials());
        }

        creditEvaluationService.evaluate(application.getId());

        return getApplicationDetail(application.getId(), null);
    }

    @Override
    public List<LoanApplicationDTO> listMyApplications(Long customerId) {
        QueryWrapper<LoanApplication> wrapper = new QueryWrapper<>();
        wrapper.eq("customer_id", customerId).orderByDesc("create_time");
        List<LoanApplication> list = loanApplicationMapper.selectList(wrapper);
        return list.stream().map(this::toBriefDto).collect(Collectors.toList());
    }

    @Override
    public LoanApplicationDTO getApplicationDetail(Long applicationId, Long customerId) {
        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (customerId != null && !customerId.equals(application.getCustomerId())) {
            throw new RuntimeException("无权查看该申请");
        }
        return toDetailDto(application);
    }

    @Override
    public List<LoanApplicationDTO> listApplications(String status) {
        QueryWrapper<LoanApplication> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");
        List<LoanApplication> list = loanApplicationMapper.selectList(wrapper);
        return list.stream().map(this::toDetailDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMaterials(Long applicationId, Long customerId, List<ApplicationMaterialDTO> materials) {
        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!customerId.equals(application.getCustomerId())) {
            throw new RuntimeException("无权操作该申请");
        }
        if (CollectionUtils.isEmpty(materials)) {
            throw new RuntimeException("材料不能为空");
        }
        String status = application.getStatus();
        boolean isMaterialSupplement = ApplicationStatus.MATERIAL_SUPPLEMENT.name().equals(status);
        if (!ApplicationStatus.SUBMITTED.name().equals(status) && !isMaterialSupplement) {
            throw new RuntimeException("当前申请状态不允许补充材料");
        }
        saveMaterials(applicationId, materials);
        if (isMaterialSupplement) {
            creditEvaluationService.evaluate(applicationId);
        }
    }

    @Override
    public List<LoanApplicationDTO> listAssignedApplications(Long managerId) {
        QueryWrapper<LoanApplication> wrapper = new QueryWrapper<>();
        wrapper.eq("manager_id", managerId).orderByDesc("create_time");
        List<LoanApplication> list = loanApplicationMapper.selectList(wrapper);
        return list.stream().map(this::toDetailDto).collect(Collectors.toList());
    }

    @Override
    public LoanApplicationDetailDTO getApplicationFullDetail(Long applicationId) {
        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        LoanApplicationDetailDTO dto = new LoanApplicationDetailDTO();
        BeanUtils.copyProperties(application, dto);
        dto.setProduct(toProductDto(application.getProductId()));
        dto.setMaterials(loadMaterials(application.getId()));

        SysUser customer = sysUserService.getById(application.getCustomerId());
        if (customer != null) {
            dto.setCustomerName(customer.getRealName());
            dto.setCustomerPhone(SensitiveDataUtil.maskPhone(customer.getPhone()));
            dto.setIdCard(SensitiveDataUtil.maskIdCard(customer.getIdCard()));
        }

        CreditEvaluationDTO creditEvaluation = creditEvaluationService.getEvaluation(applicationId, null);
        dto.setCreditEvaluation(creditEvaluation);

        DueDiligenceDTO dueDiligence = dueDiligenceService.getReport(applicationId);
        dto.setDueDiligence(dueDiligence);

        dto.setApprovalRecords(loadApprovalRecords(applicationId));
        return dto;
    }

    private void validateMaterials(List<ApplicationMaterialDTO> materials) {
        if (CollectionUtils.isEmpty(materials)) {
            throw new RuntimeException("申请材料不能为空，请上传身份证正反面、收入证明、银行流水");
        }
        Set<String> presentTypes = materials.stream()
                .filter(m -> StringUtils.hasText(m.getMaterialType()) && StringUtils.hasText(m.getFilePath()))
                .map(ApplicationMaterialDTO::getMaterialType)
                .collect(Collectors.toSet());
        if (!presentTypes.containsAll(REQUIRED_MATERIAL_TYPES)) {
            throw new RuntimeException("申请材料不完整，必须上传身份证正反面、收入证明、银行流水");
        }
    }

    private LoanProduct validateProductAndParams(Long productId, BigDecimal amount, Integer termMonths) {
        LoanProduct product = loanProductMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("产品不存在");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new RuntimeException("产品不可申请");
        }
        if (amount == null) {
            throw new RuntimeException("申请金额不能为空");
        }
        if (amount.compareTo(product.getMinAmount()) < 0 || amount.compareTo(product.getMaxAmount()) > 0) {
            throw new RuntimeException("申请金额必须在产品额度范围内：" + product.getMinAmount() + " - " + product.getMaxAmount());
        }
        if (termMonths == null) {
            throw new RuntimeException("申请期限不能为空");
        }
        if (termMonths < product.getMinTerm() || termMonths > product.getMaxTerm()) {
            throw new RuntimeException("申请期限必须在产品期限范围内：" + product.getMinTerm() + " - " + product.getMaxTerm());
        }
        return product;
    }

    private void saveMaterials(Long applicationId, List<ApplicationMaterialDTO> materials) {
        List<ApplicationMaterial> entities = new ArrayList<>();
        for (ApplicationMaterialDTO material : materials) {
            if (!StringUtils.hasText(material.getMaterialType()) || !StringUtils.hasText(material.getFilePath())) {
                continue;
            }
            ApplicationMaterial entity = new ApplicationMaterial();
            entity.setApplicationId(applicationId);
            entity.setMaterialType(material.getMaterialType());
            entity.setFileName(material.getFileName());
            entity.setFilePath(material.getFilePath());
            entity.setUploadTime(LocalDateTime.now());
            entities.add(entity);
        }
        if (!entities.isEmpty()) {
            for (ApplicationMaterial entity : entities) {
                applicationMaterialMapper.insert(entity);
            }
        }
    }

    private String generateApplicationNo() {
        String datePrefix = "LA" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        QueryWrapper<LoanApplication> wrapper = new QueryWrapper<>();
        wrapper.likeRight("application_no", datePrefix).orderByDesc("application_no").last("limit 1");
        LoanApplication last = loanApplicationMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null && last.getApplicationNo() != null) {
            String seqStr = last.getApplicationNo().substring(datePrefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return datePrefix + String.format("%04d", seq);
    }

    private LoanApplicationDTO toBriefDto(LoanApplication application) {
        LoanApplicationDTO dto = new LoanApplicationDTO();
        BeanUtils.copyProperties(application, dto);
        dto.setProduct(toProductDto(application.getProductId()));
        return dto;
    }

    private LoanApplicationDTO toDetailDto(LoanApplication application) {
        LoanApplicationDTO dto = new LoanApplicationDTO();
        BeanUtils.copyProperties(application, dto);
        dto.setProduct(toProductDto(application.getProductId()));
        dto.setMaterials(loadMaterials(application.getId()));
        return dto;
    }

    private LoanProductDTO toProductDto(Long productId) {
        try {
            return loanProductService.getProductById(productId);
        } catch (Exception e) {
            return null;
        }
    }

    private List<ApplicationMaterialDTO> loadMaterials(Long applicationId) {
        QueryWrapper<ApplicationMaterial> wrapper = new QueryWrapper<>();
        wrapper.eq("application_id", applicationId).orderByDesc("upload_time");
        List<ApplicationMaterial> list = applicationMaterialMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(this::toMaterialDto).collect(Collectors.toList());
    }

    private List<ApprovalRecordDTO> loadApprovalRecords(Long applicationId) {
        QueryWrapper<ApprovalRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("application_id", applicationId).orderByDesc("approval_time");
        List<ApprovalRecord> list = approvalRecordMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(this::toApprovalRecordDto).collect(Collectors.toList());
    }

    private ApprovalRecordDTO toApprovalRecordDto(ApprovalRecord record) {
        ApprovalRecordDTO dto = new ApprovalRecordDTO();
        BeanUtils.copyProperties(record, dto);
        if (record.getApproverId() != null) {
            SysUser approver = sysUserMapper.selectById(record.getApproverId());
            if (approver != null) {
                dto.setApproverName(approver.getRealName());
            }
        }
        return dto;
    }

    private ApplicationMaterialDTO toMaterialDto(ApplicationMaterial material) {
        ApplicationMaterialDTO dto = new ApplicationMaterialDTO();
        BeanUtils.copyProperties(material, dto);
        return dto;
    }
}
