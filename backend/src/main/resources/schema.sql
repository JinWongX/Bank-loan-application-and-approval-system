CREATE TABLE IF NOT EXISTS sys_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    real_name TEXT,
    role TEXT NOT NULL,
    phone TEXT,
    id_card TEXT,
    age INTEGER,
    monthly_income REAL,
    credit_status TEXT,
    monthly_debt REAL,
    status INTEGER DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    role_code TEXT NOT NULL UNIQUE,
    role_name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS loan_product (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT,
    min_amount REAL,
    max_amount REAL,
    interest_rate REAL,
    min_term INTEGER,
    max_term INTEGER,
    repayment_method TEXT,
    eligibility_conditions TEXT,
    status INTEGER DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loan_application (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    application_no TEXT NOT NULL UNIQUE,
    customer_id INTEGER,
    product_id INTEGER,
    amount REAL,
    term_months INTEGER,
    purpose TEXT,
    repayment_source TEXT,
    status TEXT,
    manager_id INTEGER,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS application_material (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    application_id INTEGER,
    material_type TEXT,
    file_name TEXT,
    file_path TEXT,
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS credit_evaluation (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    application_id INTEGER,
    score INTEGER,
    grade TEXT,
    evaluation_suggestion TEXT,
    evaluate_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS due_diligence (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    application_id INTEGER,
    manager_id INTEGER,
    business_situation TEXT,
    assets_liabilities TEXT,
    repayment_ability TEXT,
    conclusion TEXT,
    attachment_path TEXT,
    submit_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS approval_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    application_id INTEGER,
    approver_id INTEGER,
    approval_level TEXT,
    action TEXT,
    comment TEXT,
    approval_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loan_contract (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    application_id INTEGER,
    contract_no TEXT,
    contract_content TEXT,
    sign_status TEXT,
    sign_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loan_disbursement (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    application_id INTEGER,
    disbursement_time DATETIME,
    amount REAL,
    account_no TEXT,
    bank_name TEXT,
    operator_id INTEGER,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS repayment_schedule (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    application_id INTEGER,
    period INTEGER,
    due_date DATE,
    principal REAL,
    interest REAL,
    total_amount REAL,
    remaining_principal REAL,
    status TEXT
);
