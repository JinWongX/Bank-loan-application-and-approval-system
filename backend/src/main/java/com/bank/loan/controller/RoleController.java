package com.bank.loan.controller;

import com.bank.loan.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    @GetMapping
    public Result<List<Map<String, Object>>> listRoles() {
        return Result.success(Arrays.asList(
                createRole("customer", "客户"),
                createRole("manager", "客户经理"),
                createRole("approver_level1", "一级审批人"),
                createRole("approver_level2", "二级审批人"),
                createRole("approver_committee", "审批委员会"),
                createRole("disbursement_officer", "放款员"),
                createRole("admin", "管理员")
        ));
    }

    private Map<String, Object> createRole(String code, String name) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("name", name);
        return map;
    }
}
