package com.bank.loan.util;

public class SensitiveDataUtil {

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        int length = phone.length();
        return phone.substring(0, 3) + "****" + phone.substring(length - 4);
    }

    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 7) {
            return idCard;
        }
        int length = idCard.length();
        StringBuilder sb = new StringBuilder();
        sb.append(idCard, 0, 3);
        for (int i = 3; i < length - 4; i++) {
            sb.append('*');
        }
        sb.append(idCard, length - 4, length);
        return sb.toString();
    }

    public static String maskBankAccount(String accountNo) {
        if (accountNo == null || accountNo.length() < 8) {
            return accountNo;
        }
        int length = accountNo.length();
        StringBuilder sb = new StringBuilder();
        sb.append(accountNo, 0, 4);
        for (int i = 4; i < length - 4; i++) {
            sb.append('*');
        }
        sb.append(accountNo, length - 4, length);
        return sb.toString();
    }
}
