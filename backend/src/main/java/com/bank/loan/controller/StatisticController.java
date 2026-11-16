package com.bank.loan.controller;

import com.bank.loan.common.Result;
import com.bank.loan.dto.StatisticDTO;
import com.bank.loan.service.StatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticController {

    @Autowired
    private StatisticService statisticService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<StatisticDTO.DashboardData> dashboard() {
        return Result.success(statisticService.getDashboardStatistics());
    }

    @GetMapping("/by-product")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<StatisticDTO.ProductStat>> byProduct() {
        return Result.success(statisticService.getProductStatistics());
    }

    @GetMapping("/by-manager")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<StatisticDTO.ManagerStat>> byManager() {
        return Result.success(statisticService.getManagerStatistics());
    }

    @GetMapping("/by-time")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<StatisticDTO.TimeStat>> byTime(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "DAY") String groupBy) {
        return Result.success(statisticService.getTimeStatistics(startDate, endDate, groupBy));
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> export(
            @RequestParam String type,
            @RequestParam String dimension,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "DAY") String groupBy) {
        byte[] data = statisticService.export(type, dimension, startDate, endDate, groupBy);
        String extension = "excel".equalsIgnoreCase(type) ? "xlsx" : "csv";
        String filename = dimension + "_statistics." + extension;
        MediaType mediaType = "excel".equalsIgnoreCase(type)
                ? MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.valueOf("text/csv;charset=UTF-8");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(data);
    }
}
