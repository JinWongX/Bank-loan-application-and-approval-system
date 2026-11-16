package com.bank.loan;

import com.bank.loan.dto.CreateUserRequest;
import com.bank.loan.dto.UpdateUserRequest;
import com.bank.loan.dto.UserDTO;
import com.bank.loan.entity.SysUser;
import com.bank.loan.mapper.SysUserMapper;
import com.bank.loan.service.impl.UserServiceImpl;
import com.bank.loan.util.AesUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AesUtil aesUtil;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        when(aesUtil.encrypt(anyString())).thenAnswer(invocation -> "enc:" + invocation.getArgument(0));
        when(aesUtil.decrypt(anyString())).thenAnswer(invocation -> invocation.getArgument(0).toString().replace("enc:", ""));
    }

    @Test
    void createUser_withValidRequest_shouldSucceed() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setPassword("123456");
        request.setRealName("测试用户");
        request.setRole("customer");
        request.setPhone("13800138000");
        request.setStatus(1);

        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("encodedPassword");

        userService.createUser(request);

        verify(sysUserMapper, times(1)).insert(any(SysUser.class));
    }

    @Test
    void createUser_withDuplicateUsername_shouldThrow() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setPassword("123456");
        request.setRole("customer");

        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.createUser(request));
        assertEquals("用户名已存在", exception.getMessage());
    }

    @Test
    void createUser_withInvalidRole_shouldThrow() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setPassword("123456");
        request.setRole("invalid_role");

        when(sysUserMapper.selectCount(any())).thenReturn(0L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.createUser(request));
        assertTrue(exception.getMessage().contains("无效的角色"));
    }

    @Test
    void createUser_withShortPassword_shouldThrow() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setPassword("123");
        request.setRole("customer");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.createUser(request));
        assertEquals("密码长度不能小于6位", exception.getMessage());
    }

    @Test
    void updateUserStatus_shouldSucceed() {
        when(sysUserMapper.selectById(1L)).thenReturn(new SysUser());

        userService.updateStatus(1L, 0);

        verify(sysUserMapper, times(1)).updateById(any(SysUser.class));
    }

    @Test
    void resetPassword_withValidPassword_shouldSucceed() {
        when(sysUserMapper.selectById(1L)).thenReturn(new SysUser());
        when(passwordEncoder.encode("newpass123")).thenReturn("encodedNewPass");

        userService.resetPassword(1L, "newpass123");

        verify(sysUserMapper, times(1)).updateById(any(SysUser.class));
    }

    @Test
    void deleteUser_shouldSucceed() {
        when(sysUserMapper.selectById(1L)).thenReturn(new SysUser());

        userService.deleteUser(1L);

        verify(sysUserMapper, times(1)).deleteById(1L);
    }

    @Test
    void listUsers_shouldReturnMaskedSensitiveData() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRealName("测试");
        user.setRole("customer");
        user.setPhone("enc:13800138000");
        user.setIdCard("enc:110101199001011234");
        user.setStatus(1);

        when(sysUserMapper.selectList(null)).thenReturn(Collections.singletonList(user));

        UserDTO dto = userService.listUsers().get(0);
        assertEquals("testuser", dto.getUsername());
        assertTrue(dto.getPhone().contains("*"));
        assertTrue(dto.getIdCard().contains("*"));
    }

    @Test
    void updateUser_shouldEncryptPhoneAndIdCard() {
        SysUser existing = new SysUser();
        existing.setId(1L);
        when(sysUserMapper.selectById(1L)).thenReturn(existing);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setRealName("更新姓名");
        request.setRole("manager");
        request.setPhone("13900139000");
        request.setIdCard("110101199001019999");
        request.setAge(30);
        request.setMonthlyIncome(new BigDecimal("10000"));
        request.setCreditStatus("GOOD");
        request.setMonthlyDebt(new BigDecimal("1000"));
        request.setStatus(1);

        userService.updateUser(1L, request);

        verify(sysUserMapper, times(1)).updateById(argThat(user ->
                user.getPhone().startsWith("enc:") && user.getIdCard().startsWith("enc:")
        ));
    }
}
