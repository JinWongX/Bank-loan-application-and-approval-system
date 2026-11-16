INSERT INTO sys_role (role_code, role_name) VALUES
('customer', '客户'),
('manager', '客户经理'),
('approver_level1', '一级审批人'),
('approver_level2', '二级审批人'),
('approver_committee', '审批委员会'),
('disbursement_officer', '放款员'),
('admin', '管理员');

INSERT INTO sys_user (username, password, real_name, role, phone, id_card, age, monthly_income, credit_status, monthly_debt, status, create_time) VALUES
('customer1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '张三', 'customer', 'DNKRK/WUmDQ0S903lQk2xdKG51XNfCVcLLuIsoZOncNX7Q0TScZK', 'lJU9CcWyUg2NZ30aqfqbWNLaQs8mXyWIHmJzmMjLDs5luG8612lN0VAD+NM5sQ==', 35, 15000.00, 'FAIR', 3000.00, 1, datetime('now')),
('manager1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '李经理', 'manager', 'DwnnWhvvKco0fBNkJwI68hz9J/nDgNWrNJxTDFbSy3a0dFTGtjA+', '0i7OKKIdHo01fZm7DaB20PerpsVn+4Z0CjgNQIPE7G9Yh2WwqboUJeMmB4kWBw==', 40, 25000.00, 'GOOD', 5000.00, 1, datetime('now')),
('approver1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '王审批', 'approver_level1', 'Ex1FiGU/Hg1+vKky7MYD3Tc477zLicY1618LHL3I0wQbWiJaXVQd', '6pdjxbwOBIOEwgooaNwqxFXbDscthbMC77qXXcjxPjFNKsF+L4UIFnlklXDnsA==', 45, 30000.00, 'GOOD', 4000.00, 1, datetime('now')),
('approver2', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '赵审批', 'approver_level2', '1w+QFCpZgYfJw3w6rZKAlLQ9Bz+5uu6Me4HdDA/M+5dkAzfWO/Gl', 'wdZ4ocksVGJmHWQwmi/kTAs3LrosN8W+Ib428OrRmN+nLvS+qYlsgjax2rmY9w==', 50, 35000.00, 'GOOD', 6000.00, 1, datetime('now')),
('committee1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '刘委员', 'approver_committee', '6cs8ZupuTakSuvhJwCVapeVjBj/zndySibK5NcQEkhayCG7CqMoO', 'XM2wHPkvwjKHtRCVK+5KoTa0UGmjgjUefazNnMFWbtEKyhevcsq/fA6XBp2ERg==', 55, 40000.00, 'GOOD', 0.00, 1, datetime('now')),
('officer1', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '孙放款', 'disbursement_officer', 'IQAAbaUALLaGI0lHNc2BFKw2mnqJKNagGgmx5uIR75kIsdg91a+w', 'MPKpKhBjvZEpUghJfC0o8uk9VJc37xCiY6ZEQmL8brcuGEgsMjqdcGQoQLpyEA==', 30, 20000.00, 'FAIR', 2000.00, 1, datetime('now')),
('admin', '$2b$10$421J2KtGAcBluRmVY/gUlOP/1t1ht5lZZac0zAAMoucvxMmiNkWz6', '系统管理员', 'admin', 'RfJp3m0GsiSvIGI0UWIykZzlzzQAHoFM9baXtpZV/HAXNcno0OkA', 'JI01c9V3wroqD9Lw9pxzIe5WBGWoSpuN2adziOvJ7TgooE4WUUF+Tg34ZsMsiA==', 38, 28000.00, 'GOOD', 0.00, 1, datetime('now'));

INSERT INTO loan_product (name, type, min_amount, max_amount, interest_rate, min_term, max_term, repayment_method, eligibility_conditions, status, create_time) VALUES
('个人信用贷', '信用贷款', 5000.00, 500000.00, 0.045, 6, 36, '等额本息', '年满18周岁，信用良好，有稳定收入来源', 1, datetime('now')),
('个人经营贷', '经营贷款', 100000.00, 2000000.00, 0.055, 12, 60, '先息后本', '个体工商户或小微企业主，经营满1年', 1, datetime('now')),
('房屋抵押贷', '抵押贷款', 200000.00, 10000000.00, 0.039, 12, 120, '等额本金', '拥有完全产权住房，房龄不超过30年', 1, datetime('now')),
('汽车消费贷', '消费贷款', 30000.00, 800000.00, 0.048, 12, 60, '等额本息', '购买自用车辆，首付比例不低于30%', 1, datetime('now')),
('企业流动资金贷', '企业贷款', 500000.00, 50000000.00, 0.052, 6, 36, '按季付息到期还本', '注册满2年的企业，财务状况良好', 0, datetime('now'));
