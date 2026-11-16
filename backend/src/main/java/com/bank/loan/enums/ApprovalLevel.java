package com.bank.loan.enums;

public enum ApprovalLevel {
    LEVEL1(1, "一级审批"),
    LEVEL2(2, "二级审批"),
    COMMITTEE(3, "审批委员会");

    private final int level;
    private final String label;

    ApprovalLevel(int level, String label) {
        this.level = level;
        this.label = label;
    }

    public int getLevel() {
        return level;
    }

    public String getLabel() {
        return label;
    }

    public static ApprovalLevel fromRole(String role) {
        if (role == null) {
            return null;
        }
        switch (role.toLowerCase()) {
            case "approver_level1":
                return LEVEL1;
            case "approver_level2":
                return LEVEL2;
            case "approver_committee":
                return COMMITTEE;
            default:
                return null;
        }
    }

    public static ApprovalLevel fromLevel(int level) {
        for (ApprovalLevel value : values()) {
            if (value.level == level) {
                return value;
            }
        }
        return null;
    }
}
