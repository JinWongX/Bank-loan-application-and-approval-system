package com.bank.loan.dto;

import lombok.Data;

import java.math.BigDecimal;

public class StatisticDTO {

    @Data
    public static class DashboardData {
        private Long pendingApplicationCount;
        private Long monthApplicationCount;
        private BigDecimal monthDisbursementAmount;
        private Double averageApprovalDurationHours;
    }

    @Data
    public static class ProductStat {
        private Long productId;
        private String productName;
        private Long applicationCount;
        private Long approvedCount;
        private BigDecimal disbursementAmount;
    }

    @Data
    public static class ManagerStat {
        private Long managerId;
        private String managerName;
        private Long applicationCount;
        private Double approvalRate;
        private BigDecimal disbursementAmount;
    }

    @Data
    public static class TimeStat {
        private String period;
        private Long applicationCount;
        private Long approvedCount;
        private Double approvalRate;
        private BigDecimal disbursementAmount;
        private Long rejectedCount;
    }
}
