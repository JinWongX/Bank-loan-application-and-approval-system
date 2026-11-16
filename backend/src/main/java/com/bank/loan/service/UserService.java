package com.bank.loan.service;

import com.bank.loan.dto.CreateUserRequest;
import com.bank.loan.dto.UpdateUserRequest;
import com.bank.loan.dto.UserDTO;

import java.util.List;

public interface UserService {

    List<UserDTO> listUsers();

    UserDTO getUserById(Long id);

    Long createUser(CreateUserRequest request);

    void updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);

    void resetPassword(Long id, String newPassword);

    void updateStatus(Long id, Integer status);
}
