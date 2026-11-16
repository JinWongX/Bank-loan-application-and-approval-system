package com.bank.loan.controller;

import com.bank.loan.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api")
public class MenuController {

    @GetMapping("/menu")
    public Result<List<Map<String, Object>>> getMenu(Authentication authentication) {
        String role = extractRole(authentication);
        return Result.success(buildMenus(role));
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> adminOnly() {
        return Result.success("admin only");
    }

    private String extractRole(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String auth = authority.getAuthority();
            if (auth.startsWith("ROLE_")) {
                return auth.substring(5).toLowerCase();
            }
        }
        return "";
    }

    private List<Map<String, Object>> buildMenus(String role) {
        switch (role) {
            case "customer":
                return Arrays.asList(
                        createMenu("home", "首页", "/customer/home", "home"),
                        createMenu("apply", "贷款申请", "/customer/apply", "edit"),
                        createMenu("my-loans", "我的贷款", "/customer/loans", "document"),
                        createMenu("my-contracts", "我的合同", "/customer/contracts", "document-checked"),
                        createMenu("repayment", "还款计划", "/customer/repayment", "calendar")
                );
            case "manager":
                return Arrays.asList(
                        createMenu("home", "首页", "/manager/home", "home"),
                        createMenu("pending", "待尽调申请", "/manager/pending", "search")
                );
            case "approver_level1":
            case "approver_level2":
            case "approver_committee":
                return Arrays.asList(
                        createMenu("home", "首页", "/approver/home", "home"),
                        createMenu("pending", "待审批申请", "/approver/pending", "timer")
                );
            case "disbursement_officer":
                return Arrays.asList(
                        createMenu("home", "首页", "/disburser/home", "home"),
                        createMenu("pending", "待放款申请", "/disburser/pending", "money")
                );
            case "admin":
                return Arrays.asList(
                        createMenu("home", "首页", "/admin/home", "home"),
                        createMenu("dashboard", "统计看板", "/admin/dashboard", "data-line"),
                        createMenu("products", "产品配置", "/admin/products", "goods"),
                        createMenu("users", "用户管理", "/admin/users", "user")
                );
            default:
                return Collections.emptyList();
        }
    }

    private Map<String, Object> createMenu(String name, String title, String path, String icon) {
        Map<String, Object> menu = new LinkedHashMap<>();
        menu.put("name", name);
        menu.put("title", title);
        menu.put("path", path);
        menu.put("icon", icon);
        return menu;
    }
}
