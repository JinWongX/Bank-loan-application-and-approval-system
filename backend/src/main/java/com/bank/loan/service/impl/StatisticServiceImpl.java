package com.bank.loan.service.impl;

import com.bank.loan.dto.StatisticDTO;
import com.bank.loan.entity.ApprovalRecord;
import com.bank.loan.entity.DueDiligence;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.entity.LoanDisbursement;
import com.bank.loan.entity.LoanProduct;
import com.bank.loan.entity.SysUser;
import com.bank.loan.enums.ApplicationStatus;
import com.bank.loan.enums.ApprovalAction;
import com.bank.loan.mapper.ApprovalRecordMapper;
import com.bank.loan.mapper.DueDiligenceMapper;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.bank.loan.mapper.LoanDisbursementMapper;
import com.bank.loan.mapper.LoanProductMapper;
import com.bank.loan.mapper.SysUserMapper;
import com.bank.loan.service.StatisticService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class StatisticServiceImpl implements StatisticService {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");

    private static final List<String> APPROVED_STATUSES = Arrays.asList(
            ApplicationStatus.APPROVED.name(),
            ApplicationStatus.CONTRACT_GENERATED.name(),
            ApplicationStatus.SIGNED.name(),
            ApplicationStatus.DISBURSED.name()
    );

    @Autowired
    private LoanApplicationMapper loanApplicationMapper;

    @Autowired
    private LoanDisbursementMapper loanDisbursementMapper;

    @Autowired
    private LoanProductMapper loanProductMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private DueDiligenceMapper dueDiligenceMapper;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Override
    public StatisticDTO.DashboardData getDashboardStatistics() {
        StatisticDTO.DashboardData data = new StatisticDTO.DashboardData();

        QueryWrapper<LoanApplication> pendingWrapper = new QueryWrapper<>();
        pendingWrapper.notIn("status", ApplicationStatus.REJECTED.name(), ApplicationStatus.DISBURSED.name());
        data.setPendingApplicationCount(loanApplicationMapper.selectCount(pendingWrapper));

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        QueryWrapper<LoanApplication> monthAppWrapper = new QueryWrapper<>();
        monthAppWrapper.ge("create_time", monthStart);
        data.setMonthApplicationCount(loanApplicationMapper.selectCount(monthAppWrapper));

        QueryWrapper<LoanDisbursement> monthDisWrapper = new QueryWrapper<>();
        monthDisWrapper.ge("disbursement_time", monthStart);
        List<LoanDisbursement> monthDisbursements = loanDisbursementMapper.selectList(monthDisWrapper);
        BigDecimal monthAmount = monthDisbursements.stream()
                .map(LoanDisbursement::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.setMonthDisbursementAmount(monthAmount);

        data.setAverageApprovalDurationHours(calculateAverageApprovalDuration());
        return data;
    }

    private Double calculateAverageApprovalDuration() {
        QueryWrapper<LoanApplication> wrapper = new QueryWrapper<>();
        wrapper.in("status", APPROVED_STATUSES);
        List<LoanApplication> applications = loanApplicationMapper.selectList(wrapper);
        if (applications.isEmpty()) {
            return 0.0;
        }

        List<Long> appIds = applications.stream()
                .map(LoanApplication::getId)
                .collect(Collectors.toList());

        Map<Long, LocalDateTime> dueDiligenceSubmitTimeMap = new HashMap<>();
        QueryWrapper<DueDiligence> ddWrapper = new QueryWrapper<>();
        ddWrapper.in("application_id", appIds);
        List<DueDiligence> dueDiligenceList = dueDiligenceMapper.selectList(ddWrapper);
        for (DueDiligence dd : dueDiligenceList) {
            if (dd.getSubmitTime() != null) {
                dueDiligenceSubmitTimeMap.put(dd.getApplicationId(), dd.getSubmitTime());
            }
        }

        Map<Long, LocalDateTime> firstApproveTimeMap = new HashMap<>();
        QueryWrapper<ApprovalRecord> arWrapper = new QueryWrapper<>();
        arWrapper.in("application_id", appIds)
                .eq("action", ApprovalAction.APPROVE.name());
        List<ApprovalRecord> records = approvalRecordMapper.selectList(arWrapper);
        for (ApprovalRecord record : records) {
            if (record.getApprovalTime() == null) {
                continue;
            }
            LocalDateTime existing = firstApproveTimeMap.get(record.getApplicationId());
            if (existing == null || record.getApprovalTime().isBefore(existing)) {
                firstApproveTimeMap.put(record.getApplicationId(), record.getApprovalTime());
            }
        }

        double totalHours = 0.0;
        int count = 0;
        for (LoanApplication app : applications) {
            LocalDateTime start = dueDiligenceSubmitTimeMap.get(app.getId());
            LocalDateTime end = firstApproveTimeMap.get(app.getId());
            if (start != null && end != null && !end.isBefore(start)) {
                totalHours += Duration.between(start, end).toMinutes() / 60.0;
                count++;
            }
        }

        if (count == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(totalHours / count)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Override
    public List<StatisticDTO.ProductStat> getProductStatistics() {
        List<LoanApplication> applications = loanApplicationMapper.selectList(null);
        Map<Long, LoanProduct> productMap = loadProductMap();
        Map<Long, BigDecimal> disbursementMap = loadDisbursementMap();

        Map<Long, StatisticDTO.ProductStat> result = new LinkedHashMap<>();
        for (LoanApplication app : applications) {
            Long productId = app.getProductId() == null ? 0L : app.getProductId();
            StatisticDTO.ProductStat stat = result.computeIfAbsent(productId, k -> {
                StatisticDTO.ProductStat s = new StatisticDTO.ProductStat();
                s.setProductId(k);
                LoanProduct product = productMap.get(k);
                s.setProductName(product != null ? product.getName() : "未知产品");
                s.setApplicationCount(0L);
                s.setApprovedCount(0L);
                s.setDisbursementAmount(BigDecimal.ZERO);
                return s;
            });

            stat.setApplicationCount(stat.getApplicationCount() + 1);
            if (APPROVED_STATUSES.contains(app.getStatus())) {
                stat.setApprovedCount(stat.getApprovedCount() + 1);
            }
            BigDecimal amount = disbursementMap.get(app.getId());
            if (amount != null) {
                stat.setDisbursementAmount(stat.getDisbursementAmount().add(amount));
            }
        }

        return new ArrayList<>(result.values());
    }

    @Override
    public List<StatisticDTO.ManagerStat> getManagerStatistics() {
        List<LoanApplication> applications = loanApplicationMapper.selectList(null);
        Map<Long, SysUser> userMap = loadUserMap();
        Map<Long, BigDecimal> disbursementMap = loadDisbursementMap();

        Map<Long, StatisticDTO.ManagerStat> result = new LinkedHashMap<>();
        Map<Long, Long> approvedCountMap = new HashMap<>();

        for (LoanApplication app : applications) {
            Long managerId = app.getManagerId() == null ? 0L : app.getManagerId();
            StatisticDTO.ManagerStat stat = result.computeIfAbsent(managerId, k -> {
                StatisticDTO.ManagerStat s = new StatisticDTO.ManagerStat();
                s.setManagerId(k);
                SysUser user = userMap.get(k);
                s.setManagerName(user != null ? user.getRealName() : "未分配");
                s.setApplicationCount(0L);
                s.setApprovalRate(0.0);
                s.setDisbursementAmount(BigDecimal.ZERO);
                return s;
            });

            stat.setApplicationCount(stat.getApplicationCount() + 1);
            if (APPROVED_STATUSES.contains(app.getStatus())) {
                approvedCountMap.put(managerId, approvedCountMap.getOrDefault(managerId, 0L) + 1);
            }
            BigDecimal amount = disbursementMap.get(app.getId());
            if (amount != null) {
                stat.setDisbursementAmount(stat.getDisbursementAmount().add(amount));
            }
        }

        for (StatisticDTO.ManagerStat stat : result.values()) {
            Long total = stat.getApplicationCount();
            Long approved = approvedCountMap.getOrDefault(stat.getManagerId(), 0L);
            if (total > 0) {
                double rate = approved * 100.0 / total;
                stat.setApprovalRate(BigDecimal.valueOf(rate)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue());
            }
        }

        return new ArrayList<>(result.values());
    }

    @Override
    public List<StatisticDTO.TimeStat> getTimeStatistics(LocalDate startDate, LocalDate endDate, String groupBy) {
        if (startDate == null || endDate == null) {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1);
            endDate = now;
        }
        if (groupBy == null || groupBy.trim().isEmpty()) {
            groupBy = "DAY";
        }
        String groupByUpper = groupBy.trim().toUpperCase();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        QueryWrapper<LoanApplication> wrapper = new QueryWrapper<>();
        wrapper.ge("create_time", start).lt("create_time", end);
        List<LoanApplication> applications = loanApplicationMapper.selectList(wrapper);
        Map<Long, BigDecimal> disbursementMap = loadDisbursementMap();

        Map<String, StatisticDTO.TimeStat> result = new TreeMap<>();
        for (LoanApplication app : applications) {
            if (app.getCreateTime() == null) {
                continue;
            }
            String period = formatPeriod(app.getCreateTime(), groupByUpper);
            StatisticDTO.TimeStat stat = result.computeIfAbsent(period, k -> {
                StatisticDTO.TimeStat s = new StatisticDTO.TimeStat();
                s.setPeriod(k);
                s.setApplicationCount(0L);
                s.setApprovedCount(0L);
                s.setRejectedCount(0L);
                s.setApprovalRate(0.0);
                s.setDisbursementAmount(BigDecimal.ZERO);
                return s;
            });

            stat.setApplicationCount(stat.getApplicationCount() + 1);
            if (APPROVED_STATUSES.contains(app.getStatus())) {
                stat.setApprovedCount(stat.getApprovedCount() + 1);
            } else if (ApplicationStatus.REJECTED.name().equals(app.getStatus())) {
                stat.setRejectedCount(stat.getRejectedCount() + 1);
            }
            BigDecimal amount = disbursementMap.get(app.getId());
            if (amount != null) {
                stat.setDisbursementAmount(stat.getDisbursementAmount().add(amount));
            }
        }

        for (StatisticDTO.TimeStat stat : result.values()) {
            if (stat.getApplicationCount() > 0) {
                double rate = stat.getApprovedCount() * 100.0 / stat.getApplicationCount();
                stat.setApprovalRate(BigDecimal.valueOf(rate)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue());
            }
        }

        return new ArrayList<>(result.values());
    }

    @Override
    public byte[] export(String type, String dimension, LocalDate startDate, LocalDate endDate, String groupBy) {
        if ("time".equalsIgnoreCase(dimension)) {
            if (startDate == null || endDate == null) {
                LocalDate now = LocalDate.now();
                startDate = now.withDayOfMonth(1);
                endDate = now;
            }
            if (groupBy == null || groupBy.trim().isEmpty()) {
                groupBy = "DAY";
            }
        }

        if ("excel".equalsIgnoreCase(type)) {
            return exportExcel(dimension, startDate, endDate, groupBy);
        } else if ("csv".equalsIgnoreCase(type)) {
            return exportCsv(dimension, startDate, endDate, groupBy);
        } else {
            throw new RuntimeException("不支持的导出类型：" + type);
        }
    }

    private byte[] exportExcel(String dimension, LocalDate startDate, LocalDate endDate, String groupBy) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("统计报表");
            Row headerRow = sheet.createRow(0);
            List<List<String>> rows = buildExportRows(dimension, startDate, endDate, groupBy);
            String[] headers = resolveHeaders(dimension);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (List<String> rowData : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < rowData.size(); i++) {
                    row.createCell(i).setCellValue(rowData.get(i));
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出 Excel 失败", e);
        }
    }

    private byte[] exportCsv(String dimension, LocalDate startDate, LocalDate endDate, String groupBy) {
        List<List<String>> rows = buildExportRows(dimension, startDate, endDate, groupBy);
        String[] headers = resolveHeaders(dimension);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < headers.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escapeCsv(headers[i]));
        }
        sb.append("\n");

        for (List<String> rowData : rows) {
            for (int i = 0; i < rowData.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(escapeCsv(rowData.get(i)));
            }
            sb.append("\n");
        }

        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(bom);
            out.write(content);
        } catch (Exception e) {
            throw new RuntimeException("导出 CSV 失败", e);
        }
        return out.toByteArray();
    }

    private String[] resolveHeaders(String dimension) {
        switch (dimension.toLowerCase()) {
            case "product":
                return new String[]{"产品ID", "产品名称", "申请量", "通过量", "放款金额"};
            case "manager":
                return new String[]{"经理ID", "经理名称", "申请量", "通过率(%)", "放款金额"};
            case "time":
                return new String[]{"时间段", "申请量", "通过量", "通过率(%)", "放款金额", "驳回量"};
            default:
                throw new RuntimeException("不支持的导出维度：" + dimension);
        }
    }

    private List<List<String>> buildExportRows(String dimension, LocalDate startDate, LocalDate endDate, String groupBy) {
        List<List<String>> rows = new ArrayList<>();
        switch (dimension.toLowerCase()) {
            case "product":
                for (StatisticDTO.ProductStat stat : getProductStatistics()) {
                    rows.add(Arrays.asList(
                            String.valueOf(stat.getProductId()),
                            stat.getProductName(),
                            String.valueOf(stat.getApplicationCount()),
                            String.valueOf(stat.getApprovedCount()),
                            stat.getDisbursementAmount().toString()
                    ));
                }
                break;
            case "manager":
                for (StatisticDTO.ManagerStat stat : getManagerStatistics()) {
                    rows.add(Arrays.asList(
                            String.valueOf(stat.getManagerId()),
                            stat.getManagerName(),
                            String.valueOf(stat.getApplicationCount()),
                            String.valueOf(stat.getApprovalRate()),
                            stat.getDisbursementAmount().toString()
                    ));
                }
                break;
            case "time":
                for (StatisticDTO.TimeStat stat : getTimeStatistics(startDate, endDate, groupBy)) {
                    rows.add(Arrays.asList(
                            stat.getPeriod(),
                            String.valueOf(stat.getApplicationCount()),
                            String.valueOf(stat.getApprovedCount()),
                            String.valueOf(stat.getApprovalRate()),
                            stat.getDisbursementAmount().toString(),
                            String.valueOf(stat.getRejectedCount())
                    ));
                }
                break;
            default:
                throw new RuntimeException("不支持的导出维度：" + dimension);
        }
        return rows;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatPeriod(LocalDateTime time, String groupBy) {
        switch (groupBy) {
            case "DAY":
                return time.format(DAY_FORMATTER);
            case "WEEK":
                int year = time.get(WeekFields.ISO.weekBasedYear());
                int week = time.get(WeekFields.ISO.weekOfWeekBasedYear());
                return year + "-W" + String.format("%02d", week);
            case "MONTH":
                return time.format(MONTH_FORMATTER);
            case "YEAR":
                return time.format(YEAR_FORMATTER);
            default:
                return time.format(DAY_FORMATTER);
        }
    }

    private Map<Long, LoanProduct> loadProductMap() {
        return loanProductMapper.selectList(null).stream()
                .collect(Collectors.toMap(LoanProduct::getId, p -> p, (a, b) -> a));
    }

    private Map<Long, SysUser> loadUserMap() {
        return sysUserMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
    }

    private Map<Long, BigDecimal> loadDisbursementMap() {
        return loanDisbursementMapper.selectList(null).stream()
                .filter(d -> d.getApplicationId() != null && d.getAmount() != null)
                .collect(Collectors.toMap(
                        LoanDisbursement::getApplicationId,
                        LoanDisbursement::getAmount,
                        BigDecimal::add
                ));
    }
}
