package com.bank.loan;

import com.bank.loan.util.SensitiveDataUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SensitiveDataUtilTest {

    @Test
    void maskPhone_normal() {
        assertEquals("138****8000", SensitiveDataUtil.maskPhone("13800138000"));
    }

    @Test
    void maskPhone_tooShort() {
        assertEquals("12345", SensitiveDataUtil.maskPhone("12345"));
    }

    @Test
    void maskPhone_null() {
        assertNull(SensitiveDataUtil.maskPhone(null));
    }

    @Test
    void maskIdCard_normal() {
        assertEquals("110***********1234", SensitiveDataUtil.maskIdCard("110101199001011234"));
    }

    @Test
    void maskIdCard_tooShort() {
        assertEquals("123456", SensitiveDataUtil.maskIdCard("123456"));
    }

    @Test
    void maskIdCard_null() {
        assertNull(SensitiveDataUtil.maskIdCard(null));
    }

    @Test
    void maskBankAccount_normal() {
        assertEquals("6222********7890", SensitiveDataUtil.maskBankAccount("6222021234567890"));
    }

    @Test
    void maskBankAccount_tooShort() {
        assertEquals("1234567", SensitiveDataUtil.maskBankAccount("1234567"));
    }

    @Test
    void maskBankAccount_null() {
        assertNull(SensitiveDataUtil.maskBankAccount(null));
    }
}
