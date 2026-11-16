package com.bank.loan.service;

import com.bank.loan.dto.LoginRequest;
import com.bank.loan.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
