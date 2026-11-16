package com.bank.loan;

import com.bank.loan.dto.StatisticDTO;
import com.bank.loan.service.StatisticService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class StatisticServiceTest {

    @Autowired
    private StatisticService statisticService;

    @Test
    void getDashboardStatistics_shouldReturnCorrectData() {
        StatisticDTO.DashboardData data = statisticService.getDashboardStatistics();

        assertNotNull(data);
        assertEquals(3L, data.getPendingApplicationCount());
        assertEquals(5L, data.getMonthApplicationCount());
        assertEquals(0, new BigDecimal("300000").compareTo(data.getMonthDisbursementAmount()));
        assertNotNull(data.getAverageApprovalDurationHours());
        assertTrue(data.getAverageApprovalDurationHours() >= 0.9 && data.getAverageApprovalDurationHours() <= 1.1);
    }

    @Test
    void getProductStatistics_shouldGroupByProduct() {
        List<StatisticDTO.ProductStat> stats = statisticService.getProductStatistics();

        assertFalse(stats.isEmpty());
        StatisticDTO.ProductStat product1 = stats.stream()
                .filter(s -> s.getProductId().equals(1L))
                .findFirst()
                .orElse(null);
        assertNotNull(product1);
        assertEquals("个人信用贷", product1.getProductName());
        assertEquals(4L, product1.getApplicationCount());
        assertEquals(3L, product1.getApprovedCount());
        assertEquals(0, new BigDecimal("300000").compareTo(product1.getDisbursementAmount()));
    }

    @Test
    void getManagerStatistics_shouldGroupByManager() {
        List<StatisticDTO.ManagerStat> stats = statisticService.getManagerStatistics();

        assertFalse(stats.isEmpty());
        StatisticDTO.ManagerStat manager = stats.stream()
                .filter(s -> s.getManagerId().equals(2L))
                .findFirst()
                .orElse(null);
        assertNotNull(manager);
        assertEquals("李经理", manager.getManagerName());
        assertEquals(5L, manager.getApplicationCount());
        assertEquals(80.0, manager.getApprovalRate());
        assertEquals(0, new BigDecimal("300000").compareTo(manager.getDisbursementAmount()));
    }

    @Test
    void getTimeStatistics_byDay_shouldReturn() {
        LocalDate today = LocalDate.now();
        List<StatisticDTO.TimeStat> stats = statisticService.getTimeStatistics(today, today, "DAY");

        assertEquals(1, stats.size());
        StatisticDTO.TimeStat stat = stats.get(0);
        assertEquals(today.toString(), stat.getPeriod());
        assertEquals(5L, stat.getApplicationCount());
        assertEquals(4L, stat.getApprovedCount());
        assertEquals(1L, stat.getRejectedCount());
        assertEquals(80.0, stat.getApprovalRate());
        assertEquals(0, new BigDecimal("300000").compareTo(stat.getDisbursementAmount()));
    }

    @Test
    void getTimeStatistics_byMonth_shouldReturn() {
        LocalDate today = LocalDate.now();
        List<StatisticDTO.TimeStat> stats = statisticService.getTimeStatistics(today.withDayOfMonth(1), today, "MONTH");

        assertEquals(1, stats.size());
        assertTrue(stats.get(0).getPeriod().startsWith(String.valueOf(today.getYear())));
    }

    @Test
    void getTimeStatistics_byYear_shouldReturn() {
        LocalDate today = LocalDate.now();
        List<StatisticDTO.TimeStat> stats = statisticService.getTimeStatistics(today.withDayOfYear(1), today, "YEAR");

        assertEquals(1, stats.size());
        assertEquals(String.valueOf(today.getYear()), stats.get(0).getPeriod());
    }

    @Test
    void exportExcel_shouldReturnNonEmptyBytes() {
        byte[] data = statisticService.export("excel", "product", null, null, null);

        assertNotNull(data);
        assertTrue(data.length > 0);
        assertEquals(0x50, data[0]);
        assertEquals(0x4B, data[1]);
    }

    @Test
    void exportCsv_shouldReturnUtf8Content() {
        byte[] data = statisticService.export("csv", "product", null, null, null);

        assertNotNull(data);
        String content = new String(data, StandardCharsets.UTF_8);
        assertTrue(content.contains("产品ID"));
        assertTrue(content.contains("产品名称"));
        assertTrue(content.contains("个人信用贷"));
        assertTrue(content.contains("申请量"));
    }

    @Test
    void exportUnsupportedType_shouldThrow() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> statisticService.export("pdf", "product", null, null, null));
        assertTrue(ex.getMessage().contains("不支持的导出类型"));
    }

    @Test
    void exportUnsupportedDimension_shouldThrow() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> statisticService.export("excel", "region", null, null, null));
        assertTrue(ex.getMessage().contains("导出 Excel 失败"));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("不支持的导出维度"));
    }

    @Test
    void getTimeStatistics_byWeek_shouldReturn() {
        LocalDate today = LocalDate.now();
        List<StatisticDTO.TimeStat> stats = statisticService.getTimeStatistics(today.minusDays(7), today, "WEEK");
        assertFalse(stats.isEmpty());
    }

    @Test
    void getTimeStatistics_defaultDate_shouldUseCurrentMonth() {
        List<StatisticDTO.TimeStat> stats = statisticService.getTimeStatistics(null, null, "DAY");
        assertFalse(stats.isEmpty());
    }

    @Test
    void getTimeStatistics_defaultGroupBy_shouldUseDay() {
        LocalDate today = LocalDate.now();
        List<StatisticDTO.TimeStat> stats = statisticService.getTimeStatistics(today, today, "");
        assertEquals(1, stats.size());
        assertEquals(today.toString(), stats.get(0).getPeriod());
    }

    @Test
    void exportCsv_escapeSpecialCharacters() throws Exception {
        java.lang.reflect.Method method = statisticService.getClass().getDeclaredMethod("escapeCsv", String.class);
        method.setAccessible(true);
        assertEquals("\"a,b\"", method.invoke(statisticService, "a,b"));
        assertEquals("\"a\"\"b\"", method.invoke(statisticService, "a\"b"));
        assertEquals("\"a\nb\"", method.invoke(statisticService, "a\nb"));
        assertEquals("\"a\rb\"", method.invoke(statisticService, "a\rb"));
        assertEquals("", method.invoke(statisticService, (String) null));
        assertEquals("normal", method.invoke(statisticService, "normal"));
    }
}
