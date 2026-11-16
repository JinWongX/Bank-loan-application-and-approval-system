package com.bank.loan.service.impl;

import com.bank.loan.dto.LoanApplicationDetailDTO;
import com.bank.loan.dto.LoanContractDTO;
import com.bank.loan.dto.LoanProductDTO;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.LoanContract;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.LoanContractMapper;
import com.bank.loan.service.LoanApplicationService;
import com.bank.loan.service.LoanContractService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class LoanContractServiceImpl implements LoanContractService {

    private static final String CONTRACT_STATUS_UNSIGNED = "UNSIGNED";
    private static final String CONTRACT_STATUS_SIGNED = "SIGNED";

    private static final String[] CN_NUMBERS = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
    private static final String[] CN_INTEGER_UNITS = {"", "拾", "佰", "仟"};
    private static final String[] CN_SECTION_UNITS = {"", "万", "亿", "万亿"};

    @Autowired
    private LoanApplicationMapper loanApplicationMapper;

    @Autowired
    private LoanContractMapper loanContractMapper;

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoanContractDTO generateContract(Long applicationId) {
        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!ApplicationStatus.APPROVED.name().equals(application.getStatus())) {
            throw new RuntimeException("当前申请未审批通过，无法生成合同");
        }

        LoanContract existContract = findContractByApplicationId(applicationId);
        if (existContract != null) {
            return toDto(existContract, application);
        }

        LoanApplicationDetailDTO detail = loanApplicationService.getApplicationFullDetail(applicationId);

        LoanContract contract = new LoanContract();
        contract.setApplicationId(applicationId);
        contract.setContractNo(generateContractNo());
        contract.setContractContent(buildContractContent(detail, contract.getContractNo()));
        contract.setSignStatus(CONTRACT_STATUS_UNSIGNED);
        contract.setCreateTime(LocalDateTime.now());
        loanContractMapper.insert(contract);

        application.setStatus(ApplicationStatus.CONTRACT_GENERATED.name());
        application.setUpdateTime(LocalDateTime.now());
        loanApplicationMapper.updateById(application);

        return toDto(contract, application, detail);
    }

    @Override
    public LoanContractDTO getContractByApplicationId(Long applicationId, Long customerId) {
        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (customerId != null && !customerId.equals(application.getCustomerId())) {
            throw new RuntimeException("无权查看该合同");
        }
        LoanContract contract = findContractByApplicationId(applicationId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        return toDto(contract, application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoanContractDTO signContract(Long contractId, Long customerId) {
        LoanContract contract = loanContractMapper.selectById(contractId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        LoanApplication application = loanApplicationMapper.selectById(contract.getApplicationId());
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!customerId.equals(application.getCustomerId())) {
            throw new RuntimeException("只有申请人本人才能签署合同");
        }
        if (!CONTRACT_STATUS_UNSIGNED.equals(contract.getSignStatus())) {
            throw new RuntimeException("合同已签署或状态异常");
        }
        if (!ApplicationStatus.CONTRACT_GENERATED.name().equals(application.getStatus())) {
            throw new RuntimeException("当前申请状态不允许签署");
        }

        LocalDateTime now = LocalDateTime.now();
        contract.setSignStatus(CONTRACT_STATUS_SIGNED);
        contract.setSignTime(now);
        loanContractMapper.updateById(contract);

        application.setStatus(ApplicationStatus.SIGNED.name());
        application.setUpdateTime(now);
        loanApplicationMapper.updateById(application);

        return toDto(contract, application);
    }

    @Override
    public List<LoanContractDTO> listMyContracts(Long customerId) {
        QueryWrapper<LoanApplication> wrapper = new QueryWrapper<>();
        wrapper.eq("customer_id", customerId)
                .in("status", Arrays.asList(
                        ApplicationStatus.CONTRACT_GENERATED.name(),
                        ApplicationStatus.SIGNED.name(),
                        ApplicationStatus.DISBURSED.name()))
                .orderByDesc("update_time");
        List<LoanApplication> applications = loanApplicationMapper.selectList(wrapper);
        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> applicationIds = new ArrayList<>();
        for (LoanApplication app : applications) {
            applicationIds.add(app.getId());
        }
        QueryWrapper<LoanContract> contractWrapper = new QueryWrapper<>();
        contractWrapper.in("application_id", applicationIds);
        List<LoanContract> contracts = loanContractMapper.selectList(contractWrapper);

        List<LoanContractDTO> result = new ArrayList<>();
        for (LoanApplication app : applications) {
            LoanContract matched = contracts.stream()
                    .filter(c -> c.getApplicationId().equals(app.getId()))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                result.add(toDto(matched, app));
            }
        }
        return result;
    }

    private LoanContract findContractByApplicationId(Long applicationId) {
        QueryWrapper<LoanContract> wrapper = new QueryWrapper<>();
        wrapper.eq("application_id", applicationId);
        return loanContractMapper.selectOne(wrapper);
    }

    private String generateContractNo() {
        String datePrefix = "CT" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        QueryWrapper<LoanContract> wrapper = new QueryWrapper<>();
        wrapper.likeRight("contract_no", datePrefix).orderByDesc("contract_no").last("limit 1");
        LoanContract last = loanContractMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null && last.getContractNo() != null) {
            String seqStr = last.getContractNo().substring(datePrefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return datePrefix + String.format("%04d", seq);
    }

    private String buildContractContent(LoanApplicationDetailDTO detail, String contractNo) {
        String customerName = detail.getCustomerName() == null ? "" : detail.getCustomerName();
        String idCard = detail.getIdCard() == null ? "" : detail.getIdCard();
        String productName = detail.getProduct() == null ? "" : detail.getProduct().getName();
        String repaymentMethod = detail.getProduct() == null ? "" : detail.getProduct().getRepaymentMethod();
        BigDecimal amount = detail.getAmount() == null ? BigDecimal.ZERO : detail.getAmount();
        Integer termMonths = detail.getTermMonths() == null ? 0 : detail.getTermMonths();
        BigDecimal rate = detail.getProduct() == null ? BigDecimal.ZERO : detail.getProduct().getInterestRate();
        String signDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

        StringBuilder sb = new StringBuilder();
        sb.append("银行贷款合同\n\n");
        sb.append("合同编号：").append(contractNo).append("\n\n");
        sb.append("甲方（贷款人）：XX银行\n");
        sb.append("乙方（借款人）：").append(customerName).append("\n");
        sb.append("乙方身份证号：").append(idCard).append("\n\n");
        sb.append("鉴于乙方有资金需求，向甲方申请贷款，双方经协商一致，订立本合同。\n\n");
        sb.append("一、贷款产品\n");
        sb.append("    贷款产品名称：").append(productName).append("\n");
        sb.append("    贷款金额（小写）：¥").append(amount.setScale(2, RoundingMode.HALF_UP).toPlainString()).append("\n");
        sb.append("    贷款金额（大写）：").append(convertToChineseAmount(amount)).append("\n");
        sb.append("    贷款期限：").append(termMonths).append(" 个月\n");
        sb.append("    年利率：").append(formatRate(rate)).append("\n");
        sb.append("    还款方式：").append(repaymentMethod).append("\n\n");
        sb.append("二、放款账户\n");
        sb.append("    放款账户信息待放款时由甲方填写。\n\n");
        sb.append("三、违约责任\n");
        sb.append("    1. 乙方未按约定期限归还贷款本息的，甲方有权按逾期天数计收罚息。\n");
        sb.append("    2. 乙方提供虚假资料或擅自改变贷款用途的，甲方有权提前收回贷款并追究违约责任。\n");
        sb.append("    3. 因乙方违约导致甲方采取诉讼或仲裁方式实现债权的，乙方应承担甲方因此支出的全部费用。\n\n");
        sb.append("四、签署\n");
        sb.append("    本合同自乙方电子签名之日起生效。\n");
        sb.append("    签署日期：").append(signDate).append("\n");
        return sb.toString();
    }

    private String formatRate(BigDecimal rate) {
        if (rate == null) {
            return "-";
        }
        return rate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private LoanContractDTO toDto(LoanContract contract, LoanApplication application) {
        LoanApplicationDetailDTO detail = loanApplicationService.getApplicationFullDetail(application.getId());
        return buildDto(contract, application, detail);
    }

    private LoanContractDTO toDto(LoanContract contract, LoanApplication application, LoanApplicationDetailDTO detail) {
        return buildDto(contract, application, detail);
    }

    private LoanContractDTO buildDto(LoanContract contract, LoanApplication application, LoanApplicationDetailDTO detail) {
        LoanContractDTO dto = new LoanContractDTO();
        dto.setId(contract.getId());
        dto.setApplicationId(application.getId());
        dto.setApplicationNo(application.getApplicationNo());
        dto.setContractNo(contract.getContractNo());
        dto.setContractContent(contract.getContractContent());
        dto.setSignStatus(contract.getSignStatus());
        dto.setSignTime(contract.getSignTime());
        dto.setCreateTime(contract.getCreateTime());
        dto.setAmount(application.getAmount());
        dto.setTermMonths(application.getTermMonths());
        if (detail != null) {
            dto.setCustomerName(detail.getCustomerName());
            dto.setIdCard(detail.getIdCard());
            if (detail.getProduct() != null) {
                LoanProductDTO product = detail.getProduct();
                dto.setProductName(product.getName());
                dto.setInterestRate(product.getInterestRate());
                dto.setRepaymentMethod(product.getRepaymentMethod());
            }
        }
        return dto;
    }

    private String convertToChineseAmount(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        long integerPart = amount.longValue();
        int fen = amount.movePointRight(2).intValue() % 100;

        StringBuilder result = new StringBuilder();
        if (integerPart == 0) {
            result.append("零元");
        } else {
            result.append(convertIntegerPart(integerPart)).append("元");
        }

        int jiao = fen / 10;
        int f = fen % 10;
        if (jiao == 0 && f == 0) {
            result.append("整");
        } else {
            if (jiao > 0) {
                result.append(CN_NUMBERS[jiao]).append("角");
            }
            if (f > 0) {
                result.append(CN_NUMBERS[f]).append("分");
            } else if (jiao > 0) {
                result.append("整");
            }
        }
        return result.toString();
    }

    private String convertIntegerPart(long num) {
        if (num <= 0) {
            return "";
        }
        String s = String.valueOf(num);
        int len = s.length();
        List<String> sections = new ArrayList<>();
        for (int i = len; i > 0; i -= 4) {
            int start = Math.max(0, i - 4);
            sections.add(s.substring(start, i));
        }

        StringBuilder result = new StringBuilder();
        boolean needZero = false;
        for (int i = sections.size() - 1; i >= 0; i--) {
            int section = Integer.parseInt(sections.get(i));
            if (section != 0) {
                if (needZero) {
                    result.insert(0, "零");
                }
                result.insert(0, convertFourDigitSection(section) + CN_SECTION_UNITS[i]);
                needZero = false;
            } else {
                needZero = true;
            }
        }
        return result.toString();
    }

    private String convertFourDigitSection(int num) {
        String s = String.format("%04d", num);
        StringBuilder sb = new StringBuilder();
        boolean zero = false;
        for (int i = 0; i < 4; i++) {
            int d = s.charAt(i) - '0';
            if (d == 0) {
                zero = true;
            } else {
                if (zero) {
                    sb.append("零");
                }
                sb.append(CN_NUMBERS[d]).append(CN_INTEGER_UNITS[3 - i]);
                zero = false;
            }
        }
        return sb.toString();
    }
}
