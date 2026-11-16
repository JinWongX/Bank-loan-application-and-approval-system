package com.bank.loan.service.impl;

import com.bank.loan.dto.RepaymentScheduleDTO;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.LoanDisbursement;
import com.bank.loan.entity.LoanProduct;
import com.bank.loan.entity.RepaymentSchedule;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.LoanDisbursementMapper;
import com.bank.loan.mapper.LoanProductMapper;
import com.bank.loan.mapper.RepaymentScheduleMapper;
import com.bank.loan.service.RepaymentScheduleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RepaymentScheduleServiceImpl implements RepaymentScheduleService {

    private static final String STATUS_PENDING = "PENDING";

    private static final String REPAYMENT_EQUAL_PRINCIPAL_INTEREST = "\u7B49\u989D\u672C\u606F";
    private static final String REPAYMENT_EQUAL_PRINCIPAL = "\u7B49\u989D\u672C\u91D1";
    private static final String REPAYMENT_INTEREST_FIRST = "\u5148\u606F\u540E\u672C";
    private static final String REPAYMENT_LUMP_SUM = "\u5230\u671F\u4E00\u6B21\u6027\u8FD8\u672C\u4ED8\u606F";
    private static final String REPAYMENT_QUARTERLY_INTEREST = "\u6309\u5B63\u4ED8\u606F\u5230\u671F\u8FD8\u672C";

    @Autowired
    private LoanApplicationMapper loanApplicationMapper;

    @Autowired
    private LoanProductMapper loanProductMapper;

    @Autowired
    private LoanDisbursementMapper loanDisbursementMapper;

    @Autowired
    private RepaymentScheduleMapper repaymentScheduleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateSchedule(Long applicationId) {
        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        LoanProduct product = loanProductMapper.selectById(application.getProductId());
        if (product == null) {
            throw new RuntimeException("贷款产品不存在");
        }

        QueryWrapper<RepaymentSchedule> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("application_id", applicationId);
        repaymentScheduleMapper.delete(deleteWrapper);

        BigDecimal principal = application.getAmount();
        Integer termMonths = application.getTermMonths();
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("贷款金额不合法");
        }
        if (termMonths == null || termMonths <= 0) {
            throw new RuntimeException("贷款期限不合法");
        }

        LocalDate startDate = getDisbursementDate(applicationId);
        BigDecimal annualRate = product.getInterestRate() == null ? BigDecimal.ZERO : product.getInterestRate();
        String method = product.getRepaymentMethod();

        List<RepaymentSchedule> schedules;
        switch (method) {
            case REPAYMENT_EQUAL_PRINCIPAL_INTEREST:
                schedules = buildEqualPrincipalInterest(principal, termMonths, annualRate, startDate);
                break;
            case REPAYMENT_EQUAL_PRINCIPAL:
                schedules = buildEqualPrincipal(principal, termMonths, annualRate, startDate);
                break;
            case REPAYMENT_INTEREST_FIRST:
                schedules = buildInterestFirst(principal, termMonths, annualRate, startDate);
                break;
            case REPAYMENT_LUMP_SUM:
                schedules = buildLumpSum(principal, termMonths, annualRate, startDate);
                break;
            case REPAYMENT_QUARTERLY_INTEREST:
                schedules = buildQuarterlyInterest(principal, termMonths, annualRate, startDate);
                break;
            default:
                schedules = buildLumpSum(principal, termMonths, annualRate, startDate);
                break;
        }

        for (RepaymentSchedule schedule : schedules) {
            schedule.setApplicationId(applicationId);
            schedule.setStatus(STATUS_PENDING);
            repaymentScheduleMapper.insert(schedule);
        }
    }

    @Override
    public List<RepaymentScheduleDTO> listByApplicationId(Long applicationId) {
        QueryWrapper<RepaymentSchedule> wrapper = new QueryWrapper<>();
        wrapper.eq("application_id", applicationId).orderByAsc("period");
        List<RepaymentSchedule> list = repaymentScheduleMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<RepaymentScheduleDTO> result = new ArrayList<>();
        for (RepaymentSchedule schedule : list) {
            RepaymentScheduleDTO dto = new RepaymentScheduleDTO();
            BeanUtils.copyProperties(schedule, dto);
            result.add(dto);
        }
        return result;
    }

    private LocalDate getDisbursementDate(Long applicationId) {
        QueryWrapper<LoanDisbursement> wrapper = new QueryWrapper<>();
        wrapper.eq("application_id", applicationId);
        LoanDisbursement disbursement = loanDisbursementMapper.selectOne(wrapper);
        if (disbursement != null && disbursement.getDisbursementTime() != null) {
            return disbursement.getDisbursementTime().toLocalDate();
        }
        return LocalDate.now();
    }

    private List<RepaymentSchedule> buildEqualPrincipalInterest(BigDecimal principal, int termMonths,
                                                                 BigDecimal annualRate, LocalDate startDate) {
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment = calculateEqualPrincipalInterestPayment(principal, termMonths, monthlyRate);

        List<RepaymentSchedule> schedules = new ArrayList<>();
        BigDecimal remainingPrincipal = principal;
        for (int i = 1; i <= termMonths; i++) {
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPayment = monthlyPayment.subtract(interest);
            if (i == termMonths) {
                principalPayment = remainingPrincipal;
                interest = monthlyPayment.subtract(principalPayment);
            }
            BigDecimal total = principalPayment.add(interest);
            remainingPrincipal = remainingPrincipal.subtract(principalPayment);

            schedules.add(buildSchedule(i, startDate.plusMonths(i), principalPayment, interest, total,
                    remainingPrincipal.max(BigDecimal.ZERO)));
        }
        return schedules;
    }

    private BigDecimal calculateEqualPrincipalInterestPayment(BigDecimal principal, int termMonths,
                                                              BigDecimal monthlyRate) {
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal pow = pow(onePlusRate, termMonths);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private List<RepaymentSchedule> buildEqualPrincipal(BigDecimal principal, int termMonths,
                                                        BigDecimal annualRate, LocalDate startDate) {
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        BigDecimal basePrincipal = principal.divide(new BigDecimal(termMonths), 2, RoundingMode.HALF_UP);

        List<RepaymentSchedule> schedules = new ArrayList<>();
        BigDecimal remainingPrincipal = principal;
        for (int i = 1; i <= termMonths; i++) {
            BigDecimal principalPayment = basePrincipal;
            if (i == termMonths) {
                principalPayment = remainingPrincipal;
            }
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = principalPayment.add(interest);
            remainingPrincipal = remainingPrincipal.subtract(principalPayment);

            schedules.add(buildSchedule(i, startDate.plusMonths(i), principalPayment, interest, total,
                    remainingPrincipal.max(BigDecimal.ZERO)));
        }
        return schedules;
    }

    private List<RepaymentSchedule> buildInterestFirst(BigDecimal principal, int termMonths,
                                                       BigDecimal annualRate, LocalDate startDate) {
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyInterest = principal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

        List<RepaymentSchedule> schedules = new ArrayList<>();
        BigDecimal remainingPrincipal = principal;
        for (int i = 1; i <= termMonths; i++) {
            BigDecimal principalPayment = BigDecimal.ZERO;
            BigDecimal interest = monthlyInterest;
            BigDecimal total = interest;
            if (i == termMonths) {
                principalPayment = principal;
                interest = monthlyInterest;
                total = principalPayment.add(interest);
                remainingPrincipal = BigDecimal.ZERO;
            }
            schedules.add(buildSchedule(i, startDate.plusMonths(i), principalPayment, interest, total,
                    remainingPrincipal));
        }
        return schedules;
    }

    private List<RepaymentSchedule> buildLumpSum(BigDecimal principal, int termMonths,
                                                 BigDecimal annualRate, LocalDate startDate) {
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        BigDecimal totalInterest = principal.multiply(monthlyRate)
                .multiply(new BigDecimal(termMonths)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = principal.add(totalInterest);
        LocalDate dueDate = startDate.plusMonths(termMonths);
        List<RepaymentSchedule> schedules = new ArrayList<>();
        schedules.add(buildSchedule(termMonths, dueDate, principal, totalInterest, total, BigDecimal.ZERO));
        return schedules;
    }

    private List<RepaymentSchedule> buildQuarterlyInterest(BigDecimal principal, int termMonths,
                                                             BigDecimal annualRate, LocalDate startDate) {
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        BigDecimal quarterlyInterest = principal.multiply(monthlyRate).multiply(new BigDecimal("3"))
                .setScale(2, RoundingMode.HALF_UP);

        List<RepaymentSchedule> schedules = new ArrayList<>();
        BigDecimal remainingPrincipal = principal;
        for (int i = 1; i <= termMonths; i++) {
            boolean isQuarterEnd = i % 3 == 0;
            boolean isLast = i == termMonths;
            BigDecimal interest = isQuarterEnd || isLast ? quarterlyInterest : BigDecimal.ZERO;
            BigDecimal principalPayment = BigDecimal.ZERO;
            if (isLast) {
                principalPayment = principal;
                interest = quarterlyInterest;
                remainingPrincipal = BigDecimal.ZERO;
            }
            BigDecimal total = principalPayment.add(interest);
            schedules.add(buildSchedule(i, startDate.plusMonths(i), principalPayment, interest, total,
                    remainingPrincipal));
        }
        return schedules;
    }

    private RepaymentSchedule buildSchedule(int period, LocalDate dueDate, BigDecimal principal,
                                            BigDecimal interest, BigDecimal totalAmount,
                                            BigDecimal remainingPrincipal) {
        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setPeriod(period);
        schedule.setDueDate(dueDate);
        schedule.setPrincipal(principal.setScale(2, RoundingMode.HALF_UP));
        schedule.setInterest(interest.setScale(2, RoundingMode.HALF_UP));
        schedule.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        schedule.setRemainingPrincipal(remainingPrincipal.setScale(2, RoundingMode.HALF_UP));
        return schedule;
    }

    private BigDecimal pow(BigDecimal base, int exponent) {
        BigDecimal result = BigDecimal.ONE;
        for (int i = 0; i < exponent; i++) {
            result = result.multiply(base).setScale(10, RoundingMode.HALF_UP);
        }
        return result;
    }
}
