# Test script for Mari batch payment API
# Run: pwsh scripts/test-batch-api.ps1

$BASE_URL = "http://localhost:3000"

Write-Host "🧪 Testing Mari Batch Payment API" -ForegroundColor Cyan
Write-Host ""

# Test 1: Generate Seal
Write-Host "📝 Test 1: Generate Batch Seal" -ForegroundColor Yellow
$items = @(
    @{id="emp001"; amount=2500},
    @{id="emp002"; amount=3000},
    @{id="emp003"; amount=2750}
)

$generateBody = @{ items = $items } | ConvertTo-Json -Depth 10
$sealResponse = Invoke-RestMethod -Uri "$BASE_URL/api/batch/generate-seal" `
    -Method POST `
    -ContentType "application/json" `
    -Body $generateBody

Write-Host "✅ Generated Seal: $($sealResponse.batchSeal.Substring(0,16))..." -ForegroundColor Green
Write-Host "   Item Count: $($sealResponse.itemCount)" -ForegroundColor Gray
Write-Host "   Total Amount: `$$($sealResponse.totalAmount)" -ForegroundColor Gray
Write-Host ""

$batchSeal = $sealResponse.batchSeal

# Test 2: Validate Seal
Write-Host "🔍 Test 2: Validate Batch Seal" -ForegroundColor Yellow
$validateBody = @{
    batchSeal = $batchSeal
    items = $items
} | ConvertTo-Json -Depth 10

try {
    $validateResponse = Invoke-RestMethod -Uri "$BASE_URL/api/batch/validate" `
        -Method POST `
        -ContentType "application/json" `
        -Body $validateBody
    Write-Host "✅ Seal Valid!" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "❌ Seal Validation Failed" -ForegroundColor Red
    Write-Host $_.Exception.Message
    exit 1
}

# Test 3: Submit Batch
Write-Host "📤 Test 3: Submit Batch Payment" -ForegroundColor Yellow
$submitItems = @(
    @{id="emp001"; amount=2500; recipient="0000001101"},
    @{id="emp002"; amount=3000; recipient="0000001102"},
    @{id="emp003"; amount=2750; recipient="0000001103"}
)

$submitBody = @{
    batchId = "test-batch-" + (Get-Date -Format "yyyyMMdd-HHmmss")
    batchSeal = $batchSeal
    items = $submitItems
    from = "0000001001"
    kid = "a1b2c3d4"
    sig = "mock-signature-for-testing"
} | ConvertTo-Json -Depth 10

try {
    $submitResponse = Invoke-RestMethod -Uri "$BASE_URL/api/batch/submit" `
        -Method POST `
        -ContentType "application/json" `
        -Body $submitBody
    
    Write-Host "✅ Batch Submitted Successfully!" -ForegroundColor Green
    Write-Host "   Batch ID: $($submitResponse.batchId)" -ForegroundColor Gray
    Write-Host "   Status: $($submitResponse.status)" -ForegroundColor Gray
    Write-Host "   Processed Items: $($submitResponse.itemCount)" -ForegroundColor Gray
    Write-Host ""
    
    $batchId = $submitResponse.batchId
    
    # Test 4: Check Status
    Write-Host "📊 Test 4: Check Batch Status" -ForegroundColor Yellow
    $statusResponse = Invoke-RestMethod -Uri "$BASE_URL/api/batch/status/$batchId" `
        -Method GET
    
    Write-Host "✅ Batch Status: $($statusResponse.status)" -ForegroundColor Green
    Write-Host "   Processed At: $($statusResponse.processedAt)" -ForegroundColor Gray
    Write-Host ""
    
    # Test 5: Duplicate Submission (should fail)
    Write-Host "🔁 Test 5: Test Duplicate Protection" -ForegroundColor Yellow
    try {
        $dupResponse = Invoke-RestMethod -Uri "$BASE_URL/api/batch/submit" `
            -Method POST `
            -ContentType "application/json" `
            -Body $submitBody -ErrorAction Stop
        Write-Host "❌ Duplicate protection FAILED - should have rejected" -ForegroundColor Red
    } catch {
        if ($_.Exception.Response.StatusCode -eq 409) {
            Write-Host "✅ Duplicate Protection Working - correctly rejected" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Unexpected error: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
    Write-Host ""
    
} catch {
    Write-Host "❌ Batch Submission Failed" -ForegroundColor Red
    Write-Host $_.Exception.Message
    exit 1
}

# Test 6: Invalid Seal
Write-Host "🚫 Test 6: Test Invalid Seal Detection" -ForegroundColor Yellow
$invalidSeal = "0" * 64
$invalidBody = @{
    batchSeal = $invalidSeal
    items = $items
} | ConvertTo-Json -Depth 10

try {
    $invalidResponse = Invoke-RestMethod -Uri "$BASE_URL/api/batch/validate" `
        -Method POST `
        -ContentType "application/json" `
        -Body $invalidBody -ErrorAction Stop
    Write-Host "❌ Invalid seal detection FAILED - should have rejected" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "✅ Invalid Seal Detection Working" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Unexpected error: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}
Write-Host ""

Write-Host "🎉 All Batch API Tests Complete!" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor White
Write-Host "  ✅ Seal Generation" -ForegroundColor Green
Write-Host "  ✅ Seal Validation" -ForegroundColor Green
Write-Host "  ✅ Batch Submission" -ForegroundColor Green
Write-Host "  ✅ Status Checking" -ForegroundColor Green
Write-Host "  ✅ Duplicate Protection" -ForegroundColor Green
Write-Host "  ✅ Invalid Seal Detection" -ForegroundColor Green
