package com.bank.loan;

import com.bank.loan.util.AesUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class AesUtilTest {

    @Autowired
    private AesUtil aesUtil;

    @Test
    void encryptDecrypt_shouldReturnOriginal() {
        String original = "6222021234567890123";
        String encrypted = aesUtil.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = aesUtil.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encryptNull_shouldReturnNull() {
        assertNull(aesUtil.encrypt(null));
    }

    @Test
    void decryptNull_shouldReturnNull() {
        assertNull(aesUtil.decrypt(null));
    }

    @Test
    void decryptInvalidCipher_shouldThrow() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> aesUtil.decrypt("invalid-cipher"));
        assertTrue(ex.getMessage().contains("AES decryption failed"));
    }

    @Test
    void decryptTooShort_shouldThrow() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> aesUtil.decrypt("YQ=="));
        assertTrue(ex.getMessage().contains("AES decryption failed"));
    }
}
