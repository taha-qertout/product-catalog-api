param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$productUrl = $null

function Request([string]$Method, [string]$Url, [int]$Expected, [string]$Body) {
    $options = @{ Method = $Method; Uri = $Url; SkipHttpErrorCheck = $true }
    if ($Body) { $options.ContentType = 'application/json'; $options.Body = $Body }
    $response = Invoke-WebRequest @options
    if ([int]$response.StatusCode -ne $Expected) {
        throw "$Method $Url expected $Expected, received $($response.StatusCode): $($response.Content)"
    }
    Write-Host "$Method $Url -> $Expected"
    return $response
}

try {
    $name = 'Smoke-' + [Guid]::NewGuid().ToString('N')
    $body = @{name=$name; price=49.95; stockQuantity=4} | ConvertTo-Json -Compress
    $created = Request POST "$BaseUrl/api/products" 201 $body
    $product = $created.Content | ConvertFrom-Json
    $productUrl = "$BaseUrl/api/products/$($product.id)"
    if (-not $created.Headers.Location) { throw 'Missing Location header' }
    $fetched = (Request GET $productUrl 200).Content | ConvertFrom-Json
    if ($fetched.name -ne $name -or $fetched.price -ne 49.95) { throw 'Create/read mismatch' }
    $list = (Request GET "$BaseUrl/api/products" 200).Content | ConvertFrom-Json
    if ($product.id -notin @($list.id)) { throw 'Product missing from list' }
    $body = @{name=$name; price=12.50; stockQuantity=0} | ConvertTo-Json -Compress
    $null = Request PUT $productUrl 200 $body
    $fetched = (Request GET $productUrl 200).Content | ConvertFrom-Json
    if ($fetched.price -ne 12.50 -or $fetched.stockQuantity -ne 0) { throw 'Update was not persisted' }
    $null = Request POST "$BaseUrl/api/products" 400 '{"name":"","price":-1,"stockQuantity":-1}'
    $null = Request DELETE $productUrl 204
    $null = Request GET $productUrl 404
    $null = Request PUT $productUrl 404 $body
    $null = Request DELETE $productUrl 404
    $productUrl = $null
    Write-Host 'PASS: CRUD, persistence, validation, and missing-product responses.'
} finally {
    if ($productUrl) {
        $null = Invoke-WebRequest -Method Delete -Uri $productUrl -SkipHttpErrorCheck
    }
}
