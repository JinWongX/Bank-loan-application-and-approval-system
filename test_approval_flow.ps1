$ErrorActionPreference = "Stop"

function Login($username, $password) {
    $body = @{username = $username; password = $password} | ConvertTo-Json
    $res = Invoke-RestMethod -Uri http://localhost:8080/api/auth/login -Method POST -ContentType 'application/json' -Body $body
    if ($res.code -ne 200) { throw "Login failed: $($res.msg)" }
    return $res.data.token
}

function Invoke-Api($method, $path, $token, $body = $null) {
    $headers = @{Authorization = "Bearer $token"}
    $uri = "http://localhost:8080/api$path"
    if ($body) {
        $json = $body | ConvertTo-Json -Depth 5
        return Invoke-RestMethod -Uri $uri -Method $method -Headers $headers -ContentType 'application/json' -Body $json
    }
    return Invoke-RestMethod -Uri $uri -Method $method -Headers $headers
}

# 1. Customer submits 3 applications
$customerToken = Login 'customer1' '123456'
Write-Host "Customer logged in"

$apps = @()
$amounts = @(100000, 600000, 1200000)
foreach ($amount in $amounts) {
    $productId = if ($amount -le 500000) { 1 } else { 2 }
    $body = @{
        productId = $productId
        amount = $amount
        termMonths = 12
        purpose = "Test loan $amount"
        repaymentSource = "Salary"
    }
    $res = Invoke-Api POST '/applications' $customerToken $body
    if ($res.code -ne 200) { throw "Submit failed: $($res.msg)" }
    $apps += $res.data
    Write-Host "Submitted app: id=$($res.data.id), amount=$amount, status=$($res.data.status)"
}

# 2. Manager completes due diligence
$managerToken = Login 'manager1' '123456'
Write-Host "Manager logged in"

foreach ($app in $apps) {
    $body = @{
        businessSituation = "Good"
        assetsLiabilities = "Normal"
        repaymentAbility = "Sufficient"
        conclusion = "PASS"
        attachmentPath = ""
    }
    $res = Invoke-Api POST "/due-diligence/$($app.id)" $managerToken $body
    if ($res.code -ne 200) { throw "Due diligence failed: $($res.msg)" }
    Write-Host "Due diligence done: id=$($app.id), conclusion=$($res.data.conclusion)"
}

# 3. Check application statuses after due diligence
foreach ($app in $apps) {
    $res = Invoke-Api GET "/applications/$($app.id)" $customerToken
    Write-Host "After DD status: id=$($app.id), status=$($res.data.status)"
}

# 4. Level 1 approver approves all
$level1Token = Login 'approver1' '123456'
Write-Host "Level1 approver logged in"

$pending = Invoke-Api GET '/approvals/pending' $level1Token
Write-Host "Level1 pending count: $($pending.data.Count)"

foreach ($app in $apps) {
    $res = Invoke-Api POST "/approvals/$($app.id)" $level1Token @{action = 'APPROVE'; comment = 'Level1 approved'}
    if ($res.code -ne 200) { throw "Level1 approval failed: $($res.msg)" }
    $statusRes = Invoke-Api GET "/applications/$($app.id)" $customerToken
    Write-Host "After Level1: id=$($app.id), status=$($statusRes.data.status)"
}

# 5. Level 2 approver approves 600k and 1.2m
$level2Token = Login 'approver2' '123456'
Write-Host "Level2 approver logged in"

$pending = Invoke-Api GET '/approvals/pending' $level2Token
Write-Host "Level2 pending count: $($pending.data.Count)"
foreach ($item in $pending.data) {
    Write-Host "  Pending: id=$($item.id), amount=$($item.amount), currentLevel=$($item.currentLevel)"
}

$appsToLevel2 = @($apps[1], $apps[2])
foreach ($app in $appsToLevel2) {
    $res = Invoke-Api POST "/approvals/$($app.id)" $level2Token @{action = 'APPROVE'; comment = 'Level2 approved'}
    if ($res.code -ne 200) { throw "Level2 approval failed: $($res.msg)" }
    $statusRes = Invoke-Api GET "/applications/$($app.id)" $customerToken
    Write-Host "After Level2: id=$($app.id), status=$($statusRes.data.status)"
}

# 6. Committee approves 1.2m
$committeeToken = Login 'committee1' '123456'
Write-Host "Committee logged in"

$pending = Invoke-Api GET '/approvals/pending' $committeeToken
Write-Host "Committee pending count: $($pending.data.Count)"
foreach ($item in $pending.data) {
    Write-Host "  Pending: id=$($item.id), amount=$($item.amount), currentLevel=$($item.currentLevel)"
}

$res = Invoke-Api POST "/approvals/$($apps[2].id)" $committeeToken @{action = 'APPROVE'; comment = 'Committee approved'}
if ($res.code -ne 200) { throw "Committee approval failed: $($res.msg)" }
$statusRes = Invoke-Api GET "/applications/$($apps[2].id)" $customerToken
Write-Host "After Committee: id=$($apps[2].id), status=$($statusRes.data.status)"

# 7. Query approved list
$approved = Invoke-Api GET '/applications/approved' $level1Token
Write-Host "Approved list count: $($approved.data.Count)"
foreach ($item in $approved.data) {
    Write-Host "  Approved: id=$($item.id), amount=$($item.amount), status=$($item.status)"
}

# 8. Test reject flow
Write-Host "`n--- Test REJECT ---"
$rejectAppBody = @{
    productId = 1
    amount = 80000
    termMonths = 12
    purpose = "Test reject"
    repaymentSource = "Salary"
}
$rejectApp = (Invoke-Api POST '/applications' $customerToken $rejectAppBody).data
Write-Host "Submitted reject test app: id=$($rejectApp.id)"

Invoke-Api POST "/due-diligence/$($rejectApp.id)" $managerToken @{
    businessSituation = "Average"
    assetsLiabilities = "Average"
    repaymentAbility = "Average"
    conclusion = "PASS"
    attachmentPath = ""
} | Out-Null

$rejectRes = Invoke-Api POST "/approvals/$($rejectApp.id)" $level1Token @{action = 'REJECT'; comment = 'Does not meet requirements'}
if ($rejectRes.code -ne 200) { throw "Reject failed: $($rejectRes.msg)" }
$statusRes = Invoke-Api GET "/applications/$($rejectApp.id)" $customerToken
Write-Host "After reject: id=$($rejectApp.id), status=$($statusRes.data.status)"

# 9. Test return flow
Write-Host "`n--- Test SUPPLEMENT ---"
$returnAppBody = @{
    productId = 1
    amount = 90000
    termMonths = 12
    purpose = "Test return"
    repaymentSource = "Salary"
}
$returnApp = (Invoke-Api POST '/applications' $customerToken $returnAppBody).data
Write-Host "Submitted return test app: id=$($returnApp.id)"

Invoke-Api POST "/due-diligence/$($returnApp.id)" $managerToken @{
    businessSituation = "Average"
    assetsLiabilities = "Average"
    repaymentAbility = "Average"
    conclusion = "PASS"
    attachmentPath = ""
} | Out-Null

$returnRes = Invoke-Api POST "/approvals/$($returnApp.id)" $level1Token @{action = 'SUPPLEMENT'; comment = 'Need more materials'}
if ($returnRes.code -ne 200) { throw "Return failed: $($returnRes.msg)" }
$statusRes = Invoke-Api GET "/applications/$($returnApp.id)" $customerToken
Write-Host "After return: id=$($returnApp.id), status=$($statusRes.data.status)"

Write-Host "`nAll tests completed"
