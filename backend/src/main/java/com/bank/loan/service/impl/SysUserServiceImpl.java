package com.bank.loan.service.impl;

import com.bank.loan.entity.SysUser;
import com.bank.loan.mapper.SysUserMapper;
import com.bank.loan.service.SysUserService;
import com.bank.loan.util.AesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AesUtil aesUtil;

    @Override
    public SysUser getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user != null) {
            user.setPhone(aesUtil.decrypt(user.getPhone()));
            user.setIdCard(aesUtil.decrypt(user.getIdCard()));
        }
        return user;
    }

    @Override
    public void saveUser(SysUser user) {
        if (user != null) {
            user.setPhone(aesUtil.encrypt(user.getPhone()));
            user.setIdCard(aesUtil.encrypt(user.getIdCard()));
        }
        sysUserMapper.insert(user);
    }

    @Override
    public void updateUser(SysUser user) {
        if (user != null) {
            user.setPhone(aesUtil.encrypt(user.getPhone()));
            user.setIdCard(aesUtil.encrypt(user.getIdCard()));
        }
        sysUserMapper.updateById(user);
    }
}
