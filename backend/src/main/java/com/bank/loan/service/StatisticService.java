package com.bank.loan.service;

import com.bank.loan.dto.StatisticDTO;

import java.time.LocalDate;
import java.util.List;

public interface StatisticService {

    StatisticDTO.DashboardData getDashboardStatistics();

    List<StatisticDTO.ProductStat> getProductStatistics();

    List<StatisticDTO.ManagerStat> getManagerStatistics();

    List<StatisticDTO.TimeStat> getTimeStatistics(LocalDate startDate, LocalDate endDate, String groupBy);

    byte[] export(String type, String dimension, LocalDate startDate, LocalDate endDate, String groupBy);
}
