package com.bank.loan.service;

import com.bank.loan.entity.SysUser;

public interface SysUserService {

    SysUser getById(Long id);

    void saveUser(SysUser user);

    void updateUser(SysUser user);
}
