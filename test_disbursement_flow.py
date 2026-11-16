import json
import urllib.request
import urllib.error

BASE = 'http://localhost:8080/api'

def login(username, password):
    body = json.dumps({'username': username, 'password': password}).encode('utf-8')
    req = urllib.request.Request(f'{BASE}/auth/login', data=body, headers={'Content-Type': 'application/json'})
    res = urllib.request.urlopen(req)
    data = json.loads(res.read().decode('utf-8'))
    if data['code'] != 200:
        raise RuntimeError(f'Login failed: {data}')
    return data['data']['token']

def api(method, path, token, body=None):
    headers = {'Authorization': f'Bearer {token}', 'Content-Type': 'application/json'}
    data = json.dumps(body, ensure_ascii=False).encode('utf-8') if body is not None else None
    req = urllib.request.Request(f'{BASE}{path}', data=data, headers=headers, method=method)
    res = urllib.request.urlopen(req)
    return json.loads(res.read().decode('utf-8'))

customer_token = login('customer1', '123456')
manager_token = login('manager1', '123456')
level1_token = login('approver1', '123456')
level2_token = login('approver2', '123456')
committee_token = login('committee1', '123456')
officer_token = login('officer1', '123456')
admin_token = login('admin', '123456')

lump_product = api('POST', '/products', admin_token, {
    'name': 'Lump Sum Product',
    'type': 'Credit',
    'minAmount': 10000.00,
    'maxAmount': 500000.00,
    'interestRate': 0.05,
    'minTerm': 3,
    'maxTerm': 12,
    'repaymentMethod': '到期一次性还本付息',
    'eligibilityConditions': 'Test',
    'status': 'ACTIVE'
})
print(f'Created lump-sum product id={lump_product["data"]}')

test_cases = [
    {'productId': 1, 'amount': 100000, 'termMonths': 6, 'method': '等额本息'},
    {'productId': 3, 'amount': 300000, 'termMonths': 12, 'method': '等额本金'},
    {'productId': 2, 'amount': 600000, 'termMonths': 12, 'method': '先息后本'},
    {'productId': lump_product['data'], 'amount': 100000, 'termMonths': 6, 'method': '到期一次性还本付息'},
]

apps = []
for tc in test_cases:
    body = {
        'productId': tc['productId'],
        'amount': tc['amount'],
        'termMonths': tc['termMonths'],
        'purpose': f'Test {tc["method"]}',
        'repaymentSource': 'Salary'
    }
    res = api('POST', '/applications', customer_token, body)
    if res['code'] != 200 or res['data'] is None:
        raise RuntimeError(f'Submit failed: {res}')
    app = res['data']
    print(f'Submitted app id={app["id"]} method={tc["method"]} amount={tc["amount"]} term={tc["termMonths"]}')

    api('POST', f'/due-diligence/{app["id"]}', manager_token, {
        'businessSituation': 'Good',
        'assetsLiabilities': 'Normal',
        'repaymentAbility': 'Sufficient',
        'conclusion': 'PASS',
        'attachmentPath': ''
    })

    for _ in range(5):
        status_res = api('GET', f'/applications/{app["id"]}', customer_token)
        if status_res['data']['status'] in ('APPROVED', 'CONTRACT_GENERATED'):
            break
        for token in [level1_token, level2_token, committee_token]:
            pending = api('GET', '/approvals/pending', token)
            if any(item['id'] == app['id'] for item in pending['data']):
                api('POST', f'/approvals/{app["id"]}', token, {'action': 'APPROVE', 'comment': 'approved'})
                break

    final = api('GET', f'/applications/{app["id"]}', customer_token)
    print(f'  After approvals status={final["data"]["status"]}')

    contract = api('GET', f'/contracts/{app["id"]}', customer_token)
    api('POST', f'/contracts/sign/{contract["data"]["id"]}', customer_token, None)
    signed = api('GET', f'/applications/{app["id"]}', customer_token)
    print(f'  After sign status={signed["data"]["status"]}')
    apps.append({'app': app, 'method': tc['method']})

print('\n--- Disburser pending list ---')
pending = api('GET', '/disbursements/pending', officer_token)
print(f'Pending count: {len(pending["data"])}')
for item in pending['data']:
    print(f'  appId={item["id"]} no={item["applicationNo"]} method={item["product"]["repaymentMethod"]}')

print('\n--- Disburse and schedules ---')
for entry in apps:
    app = entry['app']
    method = entry['method']
    disburse_body = {
        'disbursementTime': '2026-09-01T10:00:00',
        'amount': app['amount'],
        'accountNo': '6222021234567890123',
        'bankName': 'Test Bank'
    }
    disburse_res = api('POST', f'/disbursements/{app["id"]}', officer_token, disburse_body)
    print(f'\n[{method}] Disbursed app id={app["id"]}')

    status = api('GET', f'/applications/{app["id"]}', customer_token)
    print(f'  Application status: {status["data"]["status"]}')

    schedule = api('GET', f'/repayment-schedules/{app["id"]}', officer_token)
    if schedule.get('code') != 200 or schedule.get('data') is None:
        print(f'  Schedule response: {schedule}')
        raise RuntimeError('Failed to load schedule')
    print(f'  Schedule count: {len(schedule["data"])}')
    for s in schedule['data']:
        print(f'    period={s["period"]} due={s["dueDate"]} principal={s["principal"]} '
              f'interest={s["interest"]} total={s["totalAmount"]} remaining={s["remainingPrincipal"]}')

print('\n--- Customer repayment view ---')
customer_sched = api('GET', f'/repayment-schedules/{apps[0]["app"]["id"]}', customer_token)
print(f'Customer schedule count for first app: {len(customer_sched["data"])}')

print('\nAll disbursement tests completed')
