-- 测试数据：角色、用户、产品、申请、评估、尽调、审批、合同、放款

INSERT INTO sys_role (role_code, role_name) VALUES
('customer', '客户'),
('manager', '客户经理'),
('approver_level1', '一级审批人'),
('approver_level2', '二级审批人'),
('approver_committee', '审批委员会'),
('disbursement_officer', '放款员'),
('admin', '管理员');

-- 密码均为 BCrypt 加密后的 "123456"
INSERT INTO sys_user (id, username, password, real_name, role, phone, id_card, age, monthly_income, credit_status, monthly_debt, status, create_time) VALUES
(1, 'customer1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '张三', 'customer', 'k9RbpwTUZzQzQSspMfDn7hP5qsNwA1NumlbztLqojmyoB/58sODI', 'f5vTjwmHZH546baGiM+o1VGdlQEBrRfs1klVs1Wtn7ZXmhxJNsQ4uUK18u5nOQ==', 35, 15000.00, 'FAIR', 3000.00, 1, datetime('now')),
(2, 'manager1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '李经理', 'manager', 'kYTX37HleuA04/u2GbSopaqia/puoKKB0lIgMGLB9HPN7vYRZXpt', 'Q+2K9kfrk95zDHYjVZjv5sssXm1hw13l8G4l7zfxp/yrQXLree2wCpTsRSBIWA==', 40, 25000.00, 'GOOD', 5000.00, 1, datetime('now')),
(3, 'approver1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '王审批', 'approver_level1', 'jaNnbZuq7i7ciNy2wSYiZoD/wmirtLiCe0q+4H0YCSc7uG3KhKDz', 'j8fOP52FX4KwiqOKKa+j711C3il7f/ezCESh03VSR4DCRNt2bI1mJsi5Uo3yiQ==', 45, 30000.00, 'GOOD', 4000.00, 1, datetime('now')),
(4, 'approver2', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '赵审批', 'approver_level2', 'JuZbAXewEgyyDaAAFDeSruKfFE4zNNYOfHuuiRNW5YEHUC3Pv9dc', '86rk1iPbMGQe1f6TWFhk0PocjvDdQj2iQtb/qOH6EMKw4OfkGrbnpRcG8MYvIA==', 50, 35000.00, 'GOOD', 6000.00, 1, datetime('now')),
(5, 'committee1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '刘委员', 'approver_committee', 'mrJPxzwpqMrk/GEr4UPNn/btUGmqhsuP9qE8mjmZDhDYtcH6wRqj', '/vWY4eeolw09QM5DzHhPkSeNjxcItJz5W1cEqR2Uge2SYJYBmpdYhlwTsoadLw==', 55, 40000.00, 'GOOD', 0.00, 1, datetime('now')),
(6, 'officer1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '孙放款', 'disbursement_officer', 'lbP1hlMvteOe5KpSKjxjUGeTvmLIP555pz1tOOp0+9aphA59dBSn', 'Z7Ym/Uk8eoni6zcfSPOoFMqHjoC0RJUvv8znUjrqBvABQCJjs7lpgKcFcs63gg==', 30, 20000.00, 'FAIR', 2000.00, 1, datetime('now')),
(7, 'admin', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '系统管理员', 'admin', 'tTayVBGtVi9hBFXfit80QPS2RfzwcvudGifdErSlCZdyP8axgm3B', 'PRUddMyk/8y+N6OLumDEzzgmUyjxG/DV5dp8S160005bLcR7k7hWNQsw9WYlgg==', 38, 28000.00, 'GOOD', 0.00, 1, datetime('now'));

INSERT INTO loan_product (id, name, type, min_amount, max_amount, interest_rate, min_term, max_term, repayment_method, eligibility_conditions, status, create_time) VALUES
(1, '个人信用贷', '信用贷款', 5000.00, 500000.00, 0.045, 6, 36, '等额本息', '年满18周岁，信用良好，有稳定收入来源', 1, datetime('now')),
(2, '个人经营贷', '经营贷款', 100000.00, 2000000.00, 0.055, 12, 60, '先息后本', '个体工商户或小微企业主，经营满1年', 1, datetime('now')),
(3, '房屋抵押贷', '抵押贷款', 200000.00, 10000000.00, 0.039, 12, 120, '等额本金', '拥有完全产权住房，房龄不超过30年', 1, datetime('now')),
(4, '汽车消费贷', '消费贷款', 30000.00, 800000.00, 0.048, 12, 60, '等额本息', '购买自用车辆，首付比例不低于30%', 1, datetime('now')),
(5, '企业流动资金贷', '企业贷款', 500000.00, 50000000.00, 0.052, 6, 36, '按季付息到期还本', '注册满2年的企业，财务状况良好', 0, datetime('now'));

-- 贷款申请测试数据：覆盖审批通过、合同生成、已签署、已放款、已驳回等状态
INSERT INTO loan_application (id, application_no, customer_id, product_id, amount, term_months, purpose, repayment_source, status, manager_id, create_time, update_time) VALUES
(1, 'LA202609010001', 1, 1, 100000.00, 12, '装修', '工资', 'APPROVED', 2, datetime('now'), datetime('now')),
(2, 'LA202609010002', 1, 2, 200000.00, 24, '经营', '经营收入', 'CONTRACT_GENERATED', 2, datetime('now'), datetime('now')),
(3, 'LA202609010003', 1, 1, 50000.00, 6, '消费', '工资', 'SIGNED', 2, datetime('now'), datetime('now')),
(4, 'LA202609010004', 1, 1, 300000.00, 36, '购房', '工资', 'DISBURSED', 2, datetime('now'), datetime('now')),
(5, 'LA202609010005', 1, 1, 10000.00, 12, '其他', '工资', 'REJECTED', 2, datetime('now'), datetime('now'));

-- 尽调报告与审批记录，用于计算平均审批时长
INSERT INTO due_diligence (id, application_id, manager_id, business_situation, assets_liabilities, repayment_ability, conclusion, attachment_path, submit_time) VALUES
(1, 1, 2, '经营正常', '资产负债合理', '还款能力充足', 'PASS', '', datetime('now', '-2 hours'));

INSERT INTO approval_record (id, application_id, approver_id, approval_level, action, comment, approval_time) VALUES
(1, 1, 3, 'LEVEL1', 'APPROVE', '审批通过', datetime('now', '-1 hour'));

-- 已生成合同但未签署，用于签署测试
INSERT INTO loan_contract (id, application_id, contract_no, contract_content, sign_status, create_time) VALUES
(1, 2, 'CT202609010001', '银行贷款合同\n\n合同编号：CT202609010001\n\n甲方（贷款人）：XX银行\n乙方（借款人）：张三\n测试合同内容', 'UNSIGNED', datetime('now'));

-- 放款记录，用于统计放款额
INSERT INTO loan_disbursement (id, application_id, disbursement_time, amount, account_no, bank_name, operator_id, create_time) VALUES
(1, 4, datetime('now'), 300000.00, '6222020000000000001', '工商银行', 6, datetime('now'));
