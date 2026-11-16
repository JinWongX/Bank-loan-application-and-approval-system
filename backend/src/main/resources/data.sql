INSERT OR IGNORE INTO sys_role (role_code, role_name) VALUES
('customer', '客户'),
('manager', '客户经理'),
('approver_level1', '一级审批人'),
('approver_level2', '二级审批人'),
('approver_committee', '审批委员会'),
('disbursement_officer', '放款员'),
('admin', '管理员');

INSERT OR IGNORE INTO sys_user (username, password, real_name, role, phone, id_card, age, monthly_income, credit_status, monthly_debt, status, create_time) VALUES
('customer1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '张三', 'customer', 'k9RbpwTUZzQzQSspMfDn7hP5qsNwA1NumlbztLqojmyoB/58sODI', 'f5vTjwmHZH546baGiM+o1VGdlQEBrRfs1klVs1Wtn7ZXmhxJNsQ4uUK18u5nOQ==', 35, 15000.00, 'FAIR', 3000.00, 1, datetime('now')),
('manager1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '李经理', 'manager', 'kYTX37HleuA04/u2GbSopaqia/puoKKB0lIgMGLB9HPN7vYRZXpt', 'Q+2K9kfrk95zDHYjVZjv5sssXm1hw13l8G4l7zfxp/yrQXLree2wCpTsRSBIWA==', 40, 25000.00, 'GOOD', 5000.00, 1, datetime('now')),
('approver1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '王审批', 'approver_level1', 'jaNnbZuq7i7ciNy2wSYiZoD/wmirtLiCe0q+4H0YCSc7uG3KhKDz', 'j8fOP52FX4KwiqOKKa+j711C3il7f/ezCESh03VSR4DCRNt2bI1mJsi5Uo3yiQ==', 45, 30000.00, 'GOOD', 4000.00, 1, datetime('now')),
('approver2', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '赵审批', 'approver_level2', 'JuZbAXewEgyyDaAAFDeSruKfFE4zNNYOfHuuiRNW5YEHUC3Pv9dc', '86rk1iPbMGQe1f6TWFhk0PocjvDdQj2iQtb/qOH6EMKw4OfkGrbnpRcG8MYvIA==', 50, 35000.00, 'GOOD', 6000.00, 1, datetime('now')),
('committee1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '刘委员', 'approver_committee', 'mrJPxzwpqMrk/GEr4UPNn/btUGmqhsuP9qE8mjmZDhDYtcH6wRqj', '/vWY4eeolw09QM5DzHhPkSeNjxcItJz5W1cEqR2Uge2SYJYBmpdYhlwTsoadLw==', 55, 40000.00, 'GOOD', 0.00, 1, datetime('now')),
('officer1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '孙放款', 'disbursement_officer', 'lbP1hlMvteOe5KpSKjxjUGeTvmLIP555pz1tOOp0+9aphA59dBSn', 'Z7Ym/Uk8eoni6zcfSPOoFMqHjoC0RJUvv8znUjrqBvABQCJjs7lpgKcFcs63gg==', 30, 20000.00, 'FAIR', 2000.00, 1, datetime('now')),
('admin', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '系统管理员', 'admin', 'tTayVBGtVi9hBFXfit80QPS2RfzwcvudGifdErSlCZdyP8axgm3B', 'PRUddMyk/8y+N6OLumDEzzgmUyjxG/DV5dp8S160005bLcR7k7hWNQsw9WYlgg==', 38, 28000.00, 'GOOD', 0.00, 1, datetime('now'));

INSERT OR IGNORE INTO loan_product (id, name, type, min_amount, max_amount, interest_rate, min_term, max_term, repayment_method, eligibility_conditions, status, create_time) VALUES
(1, '个人信用贷', '信用贷款', 5000.00, 500000.00, 0.045, 6, 36, '等额本息', '年满18周岁，信用良好，有稳定收入来源', 1, datetime('now')),
(2, '个人经营贷', '经营贷款', 100000.00, 2000000.00, 0.055, 12, 60, '先息后本', '个体工商户或小微企业主，经营满1年', 1, datetime('now')),
(3, '房屋抵押贷', '抵押贷款', 200000.00, 10000000.00, 0.039, 12, 120, '等额本金', '拥有完全产权住房，房龄不超过30年', 1, datetime('now')),
(4, '汽车消费贷', '消费贷款', 30000.00, 800000.00, 0.048, 12, 60, '等额本息', '购买自用车辆，首付比例不低于30%', 1, datetime('now')),
(5, '企业流动资金贷', '企业贷款', 500000.00, 50000000.00, 0.052, 6, 36, '按季付息到期还本', '注册满2年的企业，财务状况良好', 0, datetime('now'));
