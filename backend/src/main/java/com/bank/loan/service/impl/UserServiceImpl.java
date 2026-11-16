package com.bank.loan.service.impl;

import com.bank.loan.dto.CreateUserRequest;
import com.bank.loan.dto.UpdateUserRequest;
import com.bank.loan.dto.UserDTO;
import com.bank.loan.entity.SysUser;
import com.bank.loan.mapper.SysUserMapper;
import com.bank.loan.service.UserService;
import com.bank.loan.util.AesUtil;
import com.bank.loan.util.SensitiveDataUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final List<String> VALID_ROLES = java.util.Arrays.asList(
            "customer", "manager", "approver_level1", "approver_level2", "approver_committee", "disbursement_officer", "admin"
    );

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AesUtil aesUtil;

    @Override
    public List<UserDTO> listUsers() {
        List<SysUser> list = sysUserMapper.selectList(null);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public UserDTO getUserById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return toDto(user);
    }

    @Override
    public Long createUser(CreateUserRequest request) {
        validateCreateRequest(request);
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", request.getUsername());
        if (sysUserMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(encryptIfNotBlank(request.getPhone()));
        user.setIdCard(encryptIfNotBlank(request.getIdCard()));
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        user.setCreateTime(LocalDateTime.now());
        sysUserMapper.insert(user);
        return user.getId();
    }

    @Override
    public void updateUser(Long id, UpdateUserRequest request) {
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        validateRole(request.getRole());

        SysUser user = new SysUser();
        user.setId(id);
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());
        user.setPhone(encryptIfNotBlank(request.getPhone()));
        user.setIdCard(encryptIfNotBlank(request.getIdCard()));
        user.setAge(request.getAge());
        user.setMonthlyIncome(request.getMonthlyIncome());
        user.setCreditStatus(request.getCreditStatus());
        user.setMonthlyDebt(request.getMonthlyDebt());
        user.setStatus(request.getStatus());
        sysUserMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        sysUserMapper.deleteById(id);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new RuntimeException("密码长度不能小于6位");
        }
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        if (status != 0 && status != 1) {
            throw new RuntimeException("无效的用户状态");
        }
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    private void validateCreateRequest(CreateUserRequest request) {
        if (!StringUtils.hasText(request.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }
        if (!StringUtils.hasText(request.getPassword()) || request.getPassword().length() < 6) {
            throw new RuntimeException("密码长度不能小于6位");
        }
        validateRole(request.getRole());
    }

    private void validateRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new RuntimeException("角色不能为空");
        }
        if (!VALID_ROLES.contains(role)) {
            throw new RuntimeException("无效的角色：" + role);
        }
    }

    private String encryptIfNotBlank(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return aesUtil.encrypt(value);
    }

    private UserDTO toDto(SysUser user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        if (StringUtils.hasText(user.getPhone())) {
            dto.setPhone(SensitiveDataUtil.maskPhone(aesUtil.decrypt(user.getPhone())));
        }
        if (StringUtils.hasText(user.getIdCard())) {
            dto.setIdCard(SensitiveDataUtil.maskIdCard(aesUtil.decrypt(user.getIdCard())));
        }
        return dto;
    }
}
