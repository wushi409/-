param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [switch]$StrictStop
)

$ErrorActionPreference = "Stop"

# 在 Windows PowerShell 5.1 下手动加载 HttpClient 相关程序集
Add-Type -AssemblyName System.Net.Http

# =========================
# 全局变量与工具函数
# =========================

$TimeStamp = Get-Date -Format "yyyyMMddHHmmss"
$Marker = "smoke-$TimeStamp"
$ReportDir = Join-Path $PSScriptRoot "..\test-output"
$ReportDir = [System.IO.Path]::GetFullPath($ReportDir)
$ReportFile = Join-Path $ReportDir "api-smoke-report-$TimeStamp.json"

$Results = New-Object System.Collections.Generic.List[object]

function Write-Step {
    param([string]$Text)
    Write-Host ""
    Write-Host "========== $Text ==========" -ForegroundColor Cyan
}

function Add-Result {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [bool]$Pass,
        [string]$Status = "PASS",
        [int]$BizCode = -1,
        [string]$Message = "",
        [object]$Data = $null
    )

    $item = [PSCustomObject]@{
        name      = $Name
        method    = $Method
        path      = $Path
        pass      = $Pass
        status    = $Status
        bizCode   = $BizCode
        message   = $Message
        timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
        data      = $Data
    }
    $Results.Add($item) | Out-Null

    $color = if ($Pass) { "Green" } elseif ($Status -eq "SKIP") { "Yellow" } else { "Red" }
    Write-Host ("[{0}] {1} {2} => {3} (code={4}) {5}" -f $Status, $Method, $Path, $Name, $BizCode, $Message) -ForegroundColor $color
}

function Get-Records {
    param([object]$Data)
    if ($null -eq $Data) { return @() }
    if ($Data -is [System.Array]) { return @($Data) }
    if ($Data.PSObject.Properties.Name -contains "records") { return @($Data.records) }
    return @()
}

function Invoke-Api {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [string]$Token = "",
        [object]$Body = $null,
        [int]$ExpectedBizCode = 200,
        [bool]$AllowAnyBizCode = $false
    )

    $uri = "$BaseUrl$Path"
    $headers = @{}
    if (![string]::IsNullOrWhiteSpace($Token)) {
        $headers["Authorization"] = "Bearer $Token"
    }

    try {
        if ($null -ne $Body) {
            $json = $Body | ConvertTo-Json -Depth 30
            $resp = Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -ContentType "application/json; charset=utf-8" -Body $json -TimeoutSec 60
        } else {
            $resp = Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -TimeoutSec 60
        }

        $bizCode = if ($null -ne $resp.code) { [int]$resp.code } else { -1 }
        $bizMsg = if ($null -ne $resp.message) { [string]$resp.message } else { "" }
        $pass = if ($AllowAnyBizCode) { $true } else { $bizCode -eq $ExpectedBizCode }

        Add-Result -Name $Name -Method $Method -Path $Path -Pass $pass -Status ($(if ($pass) { "PASS" } else { "FAIL" })) -BizCode $bizCode -Message $bizMsg
        if (-not $pass -and $StrictStop) {
            throw "StrictStop: $Name failed with bizCode=$bizCode message=$bizMsg"
        }

        return [PSCustomObject]@{
            ok       = $pass
            response = $resp
            data     = $resp.data
            code     = $bizCode
            message  = $bizMsg
        }
    } catch {
        $err = $_.Exception.Message
        Add-Result -Name $Name -Method $Method -Path $Path -Pass $false -Status "FAIL" -BizCode -1 -Message $err
        if ($StrictStop) {
            throw
        }
        return [PSCustomObject]@{
            ok       = $false
            response = $null
            data     = $null
            code     = -1
            message  = $err
        }
    }
}

function Invoke-Upload {
    param(
        [string]$Name,
        [string]$Path,
        [string]$FilePath
    )

    $uri = "$BaseUrl$Path"
    $client = [System.Net.Http.HttpClient]::new()
    try {
        $multipart = [System.Net.Http.MultipartFormDataContent]::new()
        $fileBytes = [System.IO.File]::ReadAllBytes($FilePath)
        $fileContent = [System.Net.Http.ByteArrayContent]::new($fileBytes)
        $ext = [System.IO.Path]::GetExtension($FilePath).ToLowerInvariant()
        $contentType = switch ($ext) {
            ".jpg" { "image/jpeg" }
            ".jpeg" { "image/jpeg" }
            ".png" { "image/png" }
            ".gif" { "image/gif" }
            ".webp" { "image/webp" }
            ".pdf" { "application/pdf" }
            default { "application/octet-stream" }
        }
        $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse($contentType)
        $multipart.Add($fileContent, "file", [System.IO.Path]::GetFileName($FilePath))

        $httpResp = $client.PostAsync($uri, $multipart).Result
        $text = $httpResp.Content.ReadAsStringAsync().Result
        $obj = $text | ConvertFrom-Json
        $bizCode = if ($null -ne $obj.code) { [int]$obj.code } else { -1 }
        $bizMsg = if ($null -ne $obj.message) { [string]$obj.message } else { "" }
        $pass = $bizCode -eq 200

        Add-Result -Name $Name -Method "POST" -Path $Path -Pass $pass -Status ($(if ($pass) { "PASS" } else { "FAIL" })) -BizCode $bizCode -Message $bizMsg
        return [PSCustomObject]@{
            ok      = $pass
            data    = $obj.data
            code    = $bizCode
            message = $bizMsg
        }
    } catch {
        Add-Result -Name $Name -Method "POST" -Path $Path -Pass $false -Status "FAIL" -BizCode -1 -Message $_.Exception.Message
        return [PSCustomObject]@{
            ok      = $false
            data    = $null
            code    = -1
            message = $_.Exception.Message
        }
    } finally {
        $client.Dispose()
    }
}

function Get-OrderIdByRemark {
    param(
        [string]$UserToken,
        [string]$Remark
    )
    $res = Invoke-Api -Name "查询用户订单(remark=$Remark)" -Method "GET" -Path "/api/orders?page=1&size=500" -Token $UserToken
    $records = Get-Records $res.data
    $found = $records | Where-Object { $_.remark -eq $Remark } | Select-Object -First 1
    if ($null -eq $found) {
        $found = $records | Where-Object { $null -ne $_.remark -and ([string]$_.remark).Contains($Remark) } | Select-Object -First 1
    }
    if ($null -eq $found) { return $null }
    return [long]$found.id
}

function Find-IdByKeyword {
    param(
        [object[]]$Records,
        [string]$Field,
        [string]$Keyword
    )
    $found = $Records | Where-Object { ($_.PSObject.Properties.Name -contains $Field) -and ($null -ne $_.$Field) -and ([string]$_.$Field).Contains($Keyword) } | Select-Object -First 1
    if ($null -eq $found) { return $null }
    return [long]$found.id
}

function Get-FirstId {
    param([object[]]$Records)
    if ($null -eq $Records -or $Records.Count -eq 0) { return $null }
    if ($null -eq $Records[0].id) { return $null }
    return [long]$Records[0].id
}

# =========================
# 前置健康检查
# =========================

Write-Step "前置检查"
$health = Invoke-Api -Name "公共接口可达性检查" -Method "GET" -Path "/api/destinations?page=1&size=1"
if (-not $health.ok) {
    throw "后端服务不可达，请先启动 Spring Boot: $BaseUrl"
}

# =========================
# 账号登录与鉴权接口
# =========================

Write-Step "登录与鉴权"

$adminLogin = Invoke-Api -Name "管理员登录" -Method "POST" -Path "/api/auth/login" -Body @{ username = "admin"; password = "123456" }
$userLogin = Invoke-Api -Name "游客登录" -Method "POST" -Path "/api/auth/login" -Body @{ username = "user1"; password = "123456" }
$providerLogin = Invoke-Api -Name "服务商登录" -Method "POST" -Path "/api/auth/login" -Body @{ username = "provider1"; password = "123456" }

if (-not $adminLogin.ok -or -not $userLogin.ok -or -not $providerLogin.ok) {
    throw "基础账号登录失败，无法继续执行全量测试。"
}

$adminToken = [string]$adminLogin.data.token
$userToken = [string]$userLogin.data.token
$providerToken = [string]$providerLogin.data.token

$adminUserId = [long]$adminLogin.data.user.id
$userUserId = [long]$userLogin.data.user.id
$providerUserId = [long]$providerLogin.data.user.id

$registerUser = "apitest_$Marker"
$registerPass = "123456"
$newPass = "1234567"

Invoke-Api -Name "用户注册" -Method "POST" -Path "/api/auth/register" -Body @{
    username = $registerUser
    password = $registerPass
    nickname = "接口测试注册用户"
    role     = 1
    status   = 1
} | Out-Null

$newLogin = Invoke-Api -Name "新注册用户登录" -Method "POST" -Path "/api/auth/login" -Body @{ username = $registerUser; password = $registerPass }
$newToken = [string]$newLogin.data.token

Invoke-Api -Name "用户信息查询 /api/auth/info" -Method "GET" -Path "/api/auth/info" -Token $newToken | Out-Null
Invoke-Api -Name "用户资料更新 /api/user/profile" -Method "PUT" -Path "/api/user/profile" -Token $newToken -Body @{
    nickname = "接口测试资料更新"
    phone    = "13900000000"
    email    = "$registerUser@test.com"
    avatar   = "/uploads/avatar/default.png"
} | Out-Null
Invoke-Api -Name "用户密码更新 /api/user/password" -Method "PUT" -Path "/api/user/password" -Token $newToken -Body @{
    oldPassword = $registerPass
    newPassword = $newPass
} | Out-Null
Invoke-Api -Name "新密码重新登录验证" -Method "POST" -Path "/api/auth/login" -Body @{ username = $registerUser; password = $newPass } | Out-Null

# 上传接口测试（不依赖 token）
# 使用 1x1 PNG 图片，满足后端文件类型校验
$tmpUpload = Join-Path $env:TEMP "api-smoke-upload-$TimeStamp.png"
$onePixelPngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII="
[System.IO.File]::WriteAllBytes($tmpUpload, [Convert]::FromBase64String($onePixelPngBase64))
Invoke-Upload -Name "文件上传 /api/upload" -Path "/api/upload" -FilePath $tmpUpload | Out-Null

# =========================
# 公共查询接口
# =========================

Write-Step "公共查询接口"

$destList = Invoke-Api -Name "目的地列表" -Method "GET" -Path "/api/destinations?page=1&size=10"
$destRecords = Get-Records $destList.data
$seedDestinationId = if ($destRecords.Count -gt 0) { [long]$destRecords[0].id } else { 1L }

Invoke-Api -Name "目的地热门列表" -Method "GET" -Path "/api/destinations/hot?limit=8" | Out-Null
Invoke-Api -Name "目的地详情" -Method "GET" -Path "/api/destinations/$seedDestinationId" | Out-Null

$productList = Invoke-Api -Name "产品列表" -Method "GET" -Path "/api/products?page=1&size=10"
$productRecords = Get-Records $productList.data
$seedProductId = if ($productRecords.Count -gt 0) { [long]$productRecords[0].id } else { 1L }

Invoke-Api -Name "产品详情" -Method "GET" -Path "/api/products/$seedProductId" -Token $userToken | Out-Null
Invoke-Api -Name "产品推荐 /api/recommend" -Method "GET" -Path "/api/recommend?limit=5" -Token $userToken | Out-Null
Invoke-Api -Name "产品评价列表 /api/reviews/product/{id}" -Method "GET" -Path "/api/reviews/product/${seedProductId}?page=1&size=5" -Token $userToken | Out-Null

# =========================
# 游客端功能
# =========================

Write-Step "游客端功能"

Invoke-Api -Name "收藏切换(添加)" -Method "POST" -Path "/api/favorites" -Token $userToken -Body @{ productId = $seedProductId } | Out-Null
Invoke-Api -Name "收藏状态检查" -Method "GET" -Path "/api/favorites/check/$seedProductId" -Token $userToken | Out-Null
Invoke-Api -Name "收藏列表查询" -Method "GET" -Path "/api/favorites" -Token $userToken | Out-Null
Invoke-Api -Name "收藏切换(恢复)" -Method "POST" -Path "/api/favorites" -Token $userToken -Body @{ productId = $seedProductId } | Out-Null

$chatMsg1 = "接口测试-游客发消息-$Marker"
$chatMsg2 = "接口测试-服务商回复-$Marker"

Invoke-Api -Name "发送聊天消息(游客->服务商)" -Method "POST" -Path "/api/chat/messages" -Token $userToken -Body @{
    receiverId = $providerUserId
    content    = $chatMsg1
    msgType    = 0
} | Out-Null
Invoke-Api -Name "发送聊天消息(服务商->游客)" -Method "POST" -Path "/api/chat/messages" -Token $providerToken -Body @{
    receiverId = $userUserId
    content    = $chatMsg2
    msgType    = 0
} | Out-Null
Invoke-Api -Name "聊天记录查询" -Method "GET" -Path "/api/chat/messages?otherId=$providerUserId" -Token $userToken | Out-Null
Invoke-Api -Name "聊天会话查询(游客)" -Method "GET" -Path "/api/chat/sessions" -Token $userToken | Out-Null
Invoke-Api -Name "聊天会话查询(服务商)" -Method "GET" -Path "/api/chat/sessions" -Token $providerToken | Out-Null

$routeResources = Invoke-Api -Name "行程资源查询" -Method "GET" -Path "/api/routes/resources?destinationId=$seedDestinationId" -Token $userToken
$attractionOptions = if ($null -ne $routeResources.data.attractions) { @($routeResources.data.attractions) } else { @() }
$hotelOptions = if ($null -ne $routeResources.data.hotels) { @($routeResources.data.hotels) } else { @() }
$transportOptions = if ($null -ne $routeResources.data.transports) { @($routeResources.data.transports) } else { @() }

$routeAttractionId1 = if ($attractionOptions.Count -gt 0) { [long]$attractionOptions[0].id } else { $null }
$routeAttractionId2 = if ($attractionOptions.Count -gt 1) { [long]$attractionOptions[1].id } else { $null }
$routeHotelId = if ($hotelOptions.Count -gt 0) { [long]$hotelOptions[0].id } else { $null }
$routeTransportId = if ($transportOptions.Count -gt 0) { [long]$transportOptions[0].id } else { $null }

$routeTitle = "api-route-$Marker"
$routeCreateBody = @{
    title         = $routeTitle
    description   = "接口自动化测试路线"
    destinationId = $seedDestinationId
    duration      = 2
    dayPlans      = @(
        @{
            dayNumber     = 1
            title         = "第1天"
            description   = "自动化测试第1天"
            attractionIds = @($(if ($routeAttractionId1) { $routeAttractionId1 }))
            hotelId       = $routeHotelId
            transportId   = $routeTransportId
        },
        @{
            dayNumber     = 2
            title         = "第2天"
            description   = "自动化测试第2天"
            attractionIds = @($(if ($routeAttractionId2) { $routeAttractionId2 } elseif ($routeAttractionId1) { $routeAttractionId1 }))
            hotelId       = $null
            transportId   = $routeTransportId
        }
    )
}

$createRouteRes = Invoke-Api -Name "创建独立路线" -Method "POST" -Path "/api/routes" -Token $userToken -Body $routeCreateBody
$routeId = if ($null -ne $createRouteRes.data) { [long]$createRouteRes.data } else { $null }
if ($null -eq $routeId) {
    $routeListFallback = Invoke-Api -Name "查询独立路线(兜底找ID)" -Method "GET" -Path "/api/routes?page=1&size=20&keyword=$routeTitle" -Token $userToken
    $routeId = Find-IdByKeyword -Records (Get-Records $routeListFallback.data) -Field "title" -Keyword $routeTitle
}

Invoke-Api -Name "独立路线列表" -Method "GET" -Path "/api/routes?page=1&size=10&keyword=$routeTitle" -Token $userToken | Out-Null
if ($routeId) {
    Invoke-Api -Name "独立路线详情" -Method "GET" -Path "/api/routes/$routeId" -Token $userToken | Out-Null
    $routeUpdateBody = @{
        title         = "$routeTitle-更新"
        description   = "更新后的路线描述"
        destinationId = $seedDestinationId
        duration      = 2
        dayPlans      = $routeCreateBody.dayPlans
    }
    Invoke-Api -Name "更新独立路线" -Method "PUT" -Path "/api/routes/$routeId" -Token $userToken -Body $routeUpdateBody | Out-Null
    Invoke-Api -Name "删除独立路线" -Method "DELETE" -Path "/api/routes/$routeId" -Token $userToken | Out-Null
} else {
    Add-Result -Name "独立路线详情/更新/删除" -Method "GET|PUT|DELETE" -Path "/api/routes/{id}" -Pass $false -Status "SKIP" -BizCode -1 -Message "未获取到路线ID，跳过后续操作"
}

# =========================
# 服务商产品、库存、订单与统计
# =========================

Write-Step "服务商功能"

$providerProductTitle = "api-product-$Marker"
$providerProductPayload = @{
    title         = $providerProductTitle
    description   = "服务商接口测试产品"
    destinationId = $seedDestinationId
    duration      = 2
    price         = 199
    originalPrice = 299
    productType   = 0
    tags          = "接口测试,自动化"
    includeItems  = "门票,导游"
    excludeItems  = "个人消费"
    stock         = 80
}

Invoke-Api -Name "服务商发布产品" -Method "POST" -Path "/api/provider/products" -Token $providerToken -Body $providerProductPayload | Out-Null
$providerProductList = Invoke-Api -Name "服务商产品列表" -Method "GET" -Path "/api/provider/products?page=1&size=20&keyword=$providerProductTitle" -Token $providerToken
$providerProductRecords = Get-Records $providerProductList.data
$providerProductId = Find-IdByKeyword -Records $providerProductRecords -Field "title" -Keyword $providerProductTitle
if (-not $providerProductId) {
    $providerProductId = Get-FirstId -Records $providerProductRecords
}

if ($providerProductId) {
    $providerUpdatePayload = @{
        id            = $providerProductId
        title         = "$providerProductTitle-更新"
        description   = "更新后的服务商产品"
        destinationId = $seedDestinationId
        duration      = 3
        price         = 219
        originalPrice = 329
        productType   = 0
        tags          = "接口测试,更新"
        includeItems  = "门票,导游,接送"
        excludeItems  = "个人消费"
        stock         = 100
    }
    Invoke-Api -Name "服务商更新产品" -Method "PUT" -Path "/api/provider/products/$providerProductId" -Token $providerToken -Body $providerUpdatePayload | Out-Null
    Invoke-Api -Name "服务商更新产品上下架状态" -Method "PUT" -Path "/api/provider/products/$providerProductId/status" -Token $providerToken -Body @{ status = 1 } | Out-Null

    $stockDate1 = (Get-Date).AddDays(2).ToString("yyyy-MM-dd")
    $stockDate2 = (Get-Date).AddDays(3).ToString("yyyy-MM-dd")
    Invoke-Api -Name "服务商按日库存批量设置" -Method "POST" -Path "/api/provider/stocks/batch" -Token $providerToken -Body @{
        productId = $providerProductId
        items     = @(
            @{ stockDate = $stockDate1; stockTotal = 10; warnThreshold = 3 },
            @{ stockDate = $stockDate2; stockTotal = 8; warnThreshold = 2 }
        )
    } | Out-Null
    Invoke-Api -Name "服务商按日库存列表" -Method "GET" -Path "/api/provider/stocks?page=1&size=10&productId=$providerProductId" -Token $providerToken | Out-Null
    Invoke-Api -Name "服务商库存预警列表" -Method "GET" -Path "/api/provider/stocks/warnings?page=1&size=10" -Token $providerToken | Out-Null
} else {
    Add-Result -Name "服务商产品后续操作" -Method "PUT|POST|GET" -Path "/api/provider/products/{id}*" -Pass $false -Status "SKIP" -BizCode -1 -Message "未获取到服务商测试产品ID，跳过库存与订单流转测试"
}

Invoke-Api -Name "服务商统计概览" -Method "GET" -Path "/api/provider/stats/overview" -Token $providerToken | Out-Null
Invoke-Api -Name "服务商订单趋势统计" -Method "GET" -Path "/api/provider/stats/orders?days=7" -Token $providerToken | Out-Null
Invoke-Api -Name "服务商热门产品统计" -Method "GET" -Path "/api/provider/stats/hot-products?limit=5" -Token $providerToken | Out-Null

# =========================
# 订单完整流转 + 评价
# =========================

Write-Step "订单与评价流转"

$order1Id = $null
$order2Id = $null
$order3Id = $null
$order4Id = $null

if ($providerProductId) {
    $travelDateA = (Get-Date).AddDays(2).ToString("yyyy-MM-dd")
    $travelDateB = (Get-Date).AddDays(3).ToString("yyyy-MM-dd")
    $travelDateC = (Get-Date).AddDays(4).ToString("yyyy-MM-dd")
    $travelDateD = (Get-Date).AddDays(5).ToString("yyyy-MM-dd")

    $remark1 = "order-complete-$Marker"
    $remark2 = "order-refund-approve-$Marker"
    $remark3 = "order-cancel-$Marker"
    $remark4 = "order-refund-reject-$Marker"

    Invoke-Api -Name "游客创建订单#1(用于完成后评价)" -Method "POST" -Path "/api/orders" -Token $userToken -Body @{
        productId    = $providerProductId
        peopleCount  = 1
        travelDate   = $travelDateA
        contactName  = "接口测试用户"
        contactPhone = "13800138001"
        remark       = $remark1
    } | Out-Null
    $order1Id = Get-OrderIdByRemark -UserToken $userToken -Remark $remark1
    if ($order1Id) {
        Invoke-Api -Name "游客支付订单#1" -Method "PUT" -Path "/api/orders/$order1Id/pay" -Token $userToken | Out-Null
        Invoke-Api -Name "服务商查看订单列表(含订单#1)" -Method "GET" -Path "/api/provider/orders?page=1&size=20" -Token $providerToken | Out-Null
        Invoke-Api -Name "服务商接单(订单#1 -> 进行中)" -Method "PUT" -Path "/api/provider/orders/$order1Id/status" -Token $providerToken -Body @{ status = 2 } | Out-Null
        Invoke-Api -Name "服务商完结(订单#1 -> 已完成)" -Method "PUT" -Path "/api/provider/orders/$order1Id/status" -Token $providerToken -Body @{ status = 3 } | Out-Null
        Invoke-Api -Name "游客查询订单详情#1" -Method "GET" -Path "/api/orders/$order1Id" -Token $userToken | Out-Null

        Invoke-Api -Name "游客发布评价(绑定订单#1)" -Method "POST" -Path "/api/reviews" -Token $userToken -Body @{
            orderId = $order1Id
            rating  = 5
            content = "接口自动化评价-$Marker"
            images  = ""
        } | Out-Null
        Invoke-Api -Name "按产品查询评价(验证评价落库)" -Method "GET" -Path "/api/reviews/product/${providerProductId}?page=1&size=10" -Token $userToken | Out-Null
    } else {
        Add-Result -Name "订单#1后续流转" -Method "PUT|GET|POST" -Path "/api/orders/{id}*" -Pass $false -Status "SKIP" -BizCode -1 -Message "未查询到订单#1 ID"
    }

    Invoke-Api -Name "游客创建订单#2(用于退款通过)" -Method "POST" -Path "/api/orders" -Token $userToken -Body @{
        productId    = $providerProductId
        peopleCount  = 1
        travelDate   = $travelDateB
        contactName  = "接口测试用户"
        contactPhone = "13800138002"
        remark       = $remark2
    } | Out-Null
    $order2Id = Get-OrderIdByRemark -UserToken $userToken -Remark $remark2
    if ($order2Id) {
        Invoke-Api -Name "游客支付订单#2" -Method "PUT" -Path "/api/orders/$order2Id/pay" -Token $userToken | Out-Null
        Invoke-Api -Name "游客申请退款#2" -Method "PUT" -Path "/api/orders/$order2Id/refund" -Token $userToken | Out-Null
        Invoke-Api -Name "服务商同意退款#2" -Method "PUT" -Path "/api/provider/orders/$order2Id/refund/approve" -Token $providerToken | Out-Null
    } else {
        Add-Result -Name "订单#2退款流转" -Method "PUT" -Path "/api/orders/{id}/refund" -Pass $false -Status "SKIP" -BizCode -1 -Message "未查询到订单#2 ID"
    }

    Invoke-Api -Name "游客创建订单#3(用于取消)" -Method "POST" -Path "/api/orders" -Token $userToken -Body @{
        productId    = $providerProductId
        peopleCount  = 1
        travelDate   = $travelDateC
        contactName  = "接口测试用户"
        contactPhone = "13800138003"
        remark       = $remark3
    } | Out-Null
    $order3Id = Get-OrderIdByRemark -UserToken $userToken -Remark $remark3
    if ($order3Id) {
        Invoke-Api -Name "游客取消订单#3(未支付)" -Method "PUT" -Path "/api/orders/$order3Id/cancel" -Token $userToken | Out-Null
    } else {
        Add-Result -Name "订单#3取消流转" -Method "PUT" -Path "/api/orders/{id}/cancel" -Pass $false -Status "SKIP" -BizCode -1 -Message "未查询到订单#3 ID"
    }

    Invoke-Api -Name "游客创建订单#4(用于退款拒绝)" -Method "POST" -Path "/api/orders" -Token $userToken -Body @{
        productId    = $providerProductId
        peopleCount  = 1
        travelDate   = $travelDateD
        contactName  = "接口测试用户"
        contactPhone = "13800138004"
        remark       = $remark4
    } | Out-Null
    $order4Id = Get-OrderIdByRemark -UserToken $userToken -Remark $remark4
    if ($order4Id) {
        Invoke-Api -Name "游客支付订单#4" -Method "PUT" -Path "/api/orders/$order4Id/pay" -Token $userToken | Out-Null
        Invoke-Api -Name "游客申请退款#4" -Method "PUT" -Path "/api/orders/$order4Id/refund" -Token $userToken | Out-Null
        Invoke-Api -Name "服务商拒绝退款#4" -Method "PUT" -Path "/api/provider/orders/$order4Id/refund/reject" -Token $providerToken | Out-Null
    } else {
        Add-Result -Name "订单#4退款拒绝流转" -Method "PUT" -Path "/api/provider/orders/{id}/refund/reject" -Pass $false -Status "SKIP" -BizCode -1 -Message "未查询到订单#4 ID"
    }

    Invoke-Api -Name "游客订单列表" -Method "GET" -Path "/api/orders?page=1&size=20" -Token $userToken | Out-Null
} else {
    Add-Result -Name "订单流转全流程" -Method "POST|PUT|GET" -Path "/api/orders/*" -Pass $false -Status "SKIP" -BizCode -1 -Message "未创建服务商测试产品，订单流转测试已跳过"
}

# =========================
# 定制需求与方案流转
# =========================

Write-Step "定制需求与方案"

$customRequestTitle1 = "custom-a-$Marker"
$customRequestTitle2 = "custom-b-$Marker"

$startDate = (Get-Date).AddDays(15).ToString("yyyy-MM-dd")
$endDate = (Get-Date).AddDays(18).ToString("yyyy-MM-dd")

Invoke-Api -Name "游客提交定制需求#1" -Method "POST" -Path "/api/custom-requests" -Token $userToken -Body @{
    destinationId = $seedDestinationId
    title         = $customRequestTitle1
    budgetMin     = 2000
    budgetMax     = 6000
    startDate     = $startDate
    endDate       = $endDate
    peopleCount   = 2
    preferences   = "轻松行程"
    interestTags  = "美食,文化"
} | Out-Null

Invoke-Api -Name "游客提交定制需求#2" -Method "POST" -Path "/api/custom-requests" -Token $userToken -Body @{
    destinationId = $seedDestinationId
    title         = $customRequestTitle2
    budgetMin     = 3000
    budgetMax     = 8000
    startDate     = $startDate
    endDate       = $endDate
    peopleCount   = 3
    preferences   = "亲子友好"
    interestTags  = "亲子,自然"
} | Out-Null

$myCustomRequests = Invoke-Api -Name "游客定制需求列表" -Method "GET" -Path "/api/custom-requests?page=1&size=20" -Token $userToken
$customRecords = Get-Records $myCustomRequests.data
$customReqId1 = Find-IdByKeyword -Records $customRecords -Field "title" -Keyword $customRequestTitle1
$customReqId2 = Find-IdByKeyword -Records $customRecords -Field "title" -Keyword $customRequestTitle2

if ($customReqId1) {
    Invoke-Api -Name "游客查看定制需求详情#1" -Method "GET" -Path "/api/custom-requests/$customReqId1" -Token $userToken | Out-Null
}

Invoke-Api -Name "服务商查看待处理定制需求" -Method "GET" -Path "/api/provider/custom-requests?page=1&size=20" -Token $providerToken | Out-Null

if ($customReqId1) {
    Invoke-Api -Name "服务商智能生成定制方案草稿#1" -Method "POST" -Path "/api/provider/custom-requests/$customReqId1/generate" -Token $providerToken | Out-Null
    Invoke-Api -Name "服务商提交定制方案#1" -Method "POST" -Path "/api/provider/custom-plans" -Token $providerToken -Body @{
        requestId   = $customReqId1
        title       = "服务商方案A-$Marker"
        description = "自动化测试方案A"
        totalPrice  = 4999
        dayPlans    = '[{"day":1,"title":"到达与休整"},{"day":2,"title":"核心景点游览"}]'
    } | Out-Null
    $planForReq1 = Invoke-Api -Name "游客查看定制方案#1" -Method "GET" -Path "/api/custom-requests/$customReqId1/plan" -Token $userToken
    $planId1 = if ($planForReq1.data) { [long]$planForReq1.data.id } else { $null }
    if ($planId1) {
        Invoke-Api -Name "游客接受定制方案#1" -Method "PUT" -Path "/api/custom-plans/$planId1/accept" -Token $userToken | Out-Null
    } else {
        Add-Result -Name "接受定制方案#1" -Method "PUT" -Path "/api/custom-plans/{id}/accept" -Pass $false -Status "SKIP" -BizCode -1 -Message "未获取到方案ID#1"
    }
}

if ($customReqId2) {
    Invoke-Api -Name "服务商智能生成定制方案草稿#2" -Method "POST" -Path "/api/provider/custom-requests/$customReqId2/generate" -Token $providerToken | Out-Null
    Invoke-Api -Name "服务商提交定制方案#2" -Method "POST" -Path "/api/provider/custom-plans" -Token $providerToken -Body @{
        requestId   = $customReqId2
        title       = "服务商方案B-$Marker"
        description = "自动化测试方案B"
        totalPrice  = 6999
        dayPlans    = '[{"day":1,"title":"城市漫游"},{"day":2,"title":"主题活动"}]'
    } | Out-Null
    $planForReq2 = Invoke-Api -Name "游客查看定制方案#2" -Method "GET" -Path "/api/custom-requests/$customReqId2/plan" -Token $userToken
    $planId2 = if ($planForReq2.data) { [long]$planForReq2.data.id } else { $null }
    if ($planId2) {
        Invoke-Api -Name "游客拒绝定制方案#2" -Method "PUT" -Path "/api/custom-plans/$planId2/reject" -Token $userToken | Out-Null
    } else {
        Add-Result -Name "拒绝定制方案#2" -Method "PUT" -Path "/api/custom-plans/{id}/reject" -Pass $false -Status "SKIP" -BizCode -1 -Message "未获取到方案ID#2"
    }
}

# =========================
# 服务商资质 + 管理员审核
# =========================

Write-Step "服务商资质与审核"

$licenseNo = "SMOKE-LIC-$TimeStamp"
Invoke-Api -Name "服务商提交资质" -Method "POST" -Path "/api/provider/qualification" -Token $providerToken -Body @{
    companyName   = "接口测试旅行社-$Marker"
    licenseNo     = $licenseNo
    licenseImage  = "/uploads/license/test.png"
    contactPerson = "接口联系人"
    contactPhone  = "13911112222"
} | Out-Null

$qualificationDetail = Invoke-Api -Name "服务商查看资质详情" -Method "GET" -Path "/api/provider/qualification" -Token $providerToken
$qualificationId = if ($qualificationDetail.data) { [long]$qualificationDetail.data.id } else { $null }

$adminQualificationList = Invoke-Api -Name "管理员查看资质列表" -Method "GET" -Path "/api/admin/qualifications?page=1&size=20&auditStatus=0" -Token $adminToken
if (-not $qualificationId) {
    $qualRecords = Get-Records $adminQualificationList.data
    $qualObj = $qualRecords | Where-Object { $_.licenseNo -eq $licenseNo } | Select-Object -First 1
    if ($qualObj) { $qualificationId = [long]$qualObj.id }
}

if ($qualificationId) {
    Invoke-Api -Name "管理员审核资质(通过)" -Method "PUT" -Path "/api/admin/qualifications/$qualificationId/audit" -Token $adminToken -Body @{
        auditStatus = 1
        auditRemark = "接口自动化审核通过"
    } | Out-Null
} else {
    Add-Result -Name "管理员审核资质" -Method "PUT" -Path "/api/admin/qualifications/{id}/audit" -Pass $false -Status "SKIP" -BizCode -1 -Message "未获取到资质ID"
}

# =========================
# 管理员用户管理
# =========================

Write-Step "管理员用户管理"

$adminManagedUsername = "admin_manage_$TimeStamp"
Invoke-Api -Name "管理员新增用户" -Method "POST" -Path "/api/admin/users" -Token $adminToken -Body @{
    username = $adminManagedUsername
    password = "123456"
    nickname = "后台新增用户"
    role     = 1
    status   = 1
    phone    = "13700001111"
    email    = "$adminManagedUsername@test.com"
} | Out-Null

$adminUserList = Invoke-Api -Name "管理员用户列表查询(keyword)" -Method "GET" -Path "/api/admin/users?page=1&size=20&keyword=$adminManagedUsername" -Token $adminToken
$adminUserRecords = Get-Records $adminUserList.data
$managedUserId = Find-IdByKeyword -Records $adminUserRecords -Field "username" -Keyword $adminManagedUsername

if ($managedUserId) {
    Invoke-Api -Name "管理员更新用户资料" -Method "PUT" -Path "/api/admin/users/$managedUserId" -Token $adminToken -Body @{
        username = $adminManagedUsername
        nickname = "后台更新用户"
        role     = 1
        status   = 1
        phone    = "13700002222"
        email    = "$adminManagedUsername@update.test"
        password = ""
    } | Out-Null
    Invoke-Api -Name "管理员禁用用户" -Method "PUT" -Path "/api/admin/users/$managedUserId/status" -Token $adminToken -Body @{ status = 0 } | Out-Null
    Invoke-Api -Name "管理员启用用户" -Method "PUT" -Path "/api/admin/users/$managedUserId/status" -Token $adminToken -Body @{ status = 1 } | Out-Null
    Invoke-Api -Name "管理员删除用户" -Method "DELETE" -Path "/api/admin/users/$managedUserId" -Token $adminToken | Out-Null
} else {
    Add-Result -Name "管理员用户更新/状态/删除" -Method "PUT|DELETE" -Path "/api/admin/users/{id}*" -Pass $false -Status "SKIP" -BizCode -1 -Message "未查询到管理员新增用户ID"
}

# =========================
# 管理员资源 CRUD
# =========================

Write-Step "管理员资源管理 CRUD"

$destName = "api-destination-$Marker"
$attrName = "api-attraction-$Marker"
$hotelName = "api-hotel-$Marker"
$transportDeparture = "api-departure-$Marker"
$productName = "api-admin-product-$Marker"

Invoke-Api -Name "管理员新增目的地" -Method "POST" -Path "/api/admin/destinations" -Token $adminToken -Body @{
    name        = $destName
    province    = "测试省"
    city        = "测试市"
    description = "自动化测试目的地"
    hotScore    = 60
    status      = 1
} | Out-Null

$adminDestList = Invoke-Api -Name "管理员目的地列表查询(keyword)" -Method "GET" -Path "/api/admin/destinations?page=1&size=20&keyword=$destName" -Token $adminToken
$adminDestRecords = Get-Records $adminDestList.data
$adminDestId = Find-IdByKeyword -Records $adminDestRecords -Field "name" -Keyword $destName
if (-not $adminDestId) {
    $adminDestId = Get-FirstId -Records $adminDestRecords
}

if ($adminDestId) {
    Invoke-Api -Name "管理员更新目的地" -Method "PUT" -Path "/api/admin/destinations/$adminDestId" -Token $adminToken -Body @{
        name        = "$destName-更新"
        province    = "测试省"
        city        = "测试市"
        description = "自动化测试目的地(更新)"
        hotScore    = 70
        status      = 1
    } | Out-Null

    Invoke-Api -Name "管理员新增景点" -Method "POST" -Path "/api/admin/attractions" -Token $adminToken -Body @{
        destinationId = $adminDestId
        name          = $attrName
        description   = "自动化测试景点"
        ticketPrice   = 88
        openTime      = "09:00-18:00"
        address       = "测试地址1号"
        tags          = "测试,景点"
        status        = 1
    } | Out-Null
    $adminAttrList = Invoke-Api -Name "管理员景点列表查询(keyword)" -Method "GET" -Path "/api/admin/attractions?page=1&size=20&destinationId=$adminDestId&keyword=$attrName" -Token $adminToken
    $adminAttrId = Find-IdByKeyword -Records (Get-Records $adminAttrList.data) -Field "name" -Keyword $attrName
    if ($adminAttrId) {
        Invoke-Api -Name "管理员更新景点" -Method "PUT" -Path "/api/admin/attractions/$adminAttrId" -Token $adminToken -Body @{
            destinationId = $adminDestId
            name          = "$attrName-更新"
            description   = "自动化测试景点(更新)"
            ticketPrice   = 99
            openTime      = "08:00-18:30"
            address       = "测试地址2号"
            tags          = "测试,景点,更新"
            status        = 1
        } | Out-Null
    } else {
        Add-Result -Name "管理员更新景点" -Method "PUT" -Path "/api/admin/attractions/{id}" -Pass $false -Status "SKIP" -BizCode -1 -Message "未获取到景点ID"
    }

    Invoke-Api -Name "管理员新增酒店" -Method "POST" -Path "/api/admin/hotels" -Token $adminToken -Body @{
        destinationId = $adminDestId
        name          = $hotelName
        starLevel     = 4
        description   = "自动化测试酒店"
        address       = "测试酒店地址"
        priceMin      = 300
        priceMax      = 600
        status        = 1
    } | Out-Null
    $adminHotelList = Invoke-Api -Name "管理员酒店列表查询(keyword)" -Method "GET" -Path "/api/admin/hotels?page=1&size=20&destinationId=$adminDestId&keyword=$hotelName" -Token $adminToken
    $adminHotelId = Find-IdByKeyword -Records (Get-Records $adminHotelList.data) -Field "name" -Keyword $hotelName
    if ($adminHotelId) {
        Invoke-Api -Name "管理员更新酒店" -Method "PUT" -Path "/api/admin/hotels/$adminHotelId" -Token $adminToken -Body @{
            destinationId = $adminDestId
            name          = "$hotelName-更新"
            starLevel     = 5
            description   = "自动化测试酒店(更新)"
            address       = "测试酒店地址-更新"
            priceMin      = 350
            priceMax      = 700
            status        = 1
        } | Out-Null
    } else {
        Add-Result -Name "管理员更新酒店" -Method "PUT" -Path "/api/admin/hotels/{id}" -Pass $false -Status "SKIP" -BizCode -1 -Message "未获取到酒店ID"
    }

    Invoke-Api -Name "管理员新增交通" -Method "POST" -Path "/api/admin/transports" -Token $adminToken -Body @{
        type        = 1
        departure   = $transportDeparture
        arrival     = "接口目的地站"
        price       = 199
        description = "自动化测试交通"
        status      = 1
    } | Out-Null
    $adminTransportList = Invoke-Api -Name "管理员交通列表查询(departure)" -Method "GET" -Path "/api/admin/transports?page=1&size=20&departure=$transportDeparture" -Token $adminToken
    $transportRecords = Get-Records $adminTransportList.data
    $adminTransportObj = $transportRecords | Where-Object { $_.departure -eq $transportDeparture } | Select-Object -First 1
    $adminTransportId = if ($adminTransportObj) { [long]$adminTransportObj.id } else { $null }
    if ($adminTransportId) {
        Invoke-Api -Name "管理员更新交通" -Method "PUT" -Path "/api/admin/transports/$adminTransportId" -Token $adminToken -Body @{
            type        = 1
            departure   = "$transportDeparture-更新"
            arrival     = "接口目的地站-更新"
            price       = 220
            description = "自动化测试交通(更新)"
            status      = 1
        } | Out-Null
    } else {
        Add-Result -Name "管理员更新交通" -Method "PUT" -Path "/api/admin/transports/{id}" -Pass $false -Status "SKIP" -BizCode -1 -Message "未获取到交通ID"
    }

    Invoke-Api -Name "管理员新增产品" -Method "POST" -Path "/api/admin/products" -Token $adminToken -Body @{
        providerId    = $providerUserId
        title         = $productName
        description   = "后台新增产品"
        destinationId = $adminDestId
        duration      = 2
        price         = 399
        originalPrice = 499
        productType   = 0
        tags          = "后台,测试"
        includeItems  = "门票"
        excludeItems  = "餐饮"
        stock         = 30
        sales         = 0
        status        = 1
    } | Out-Null
    $adminProductList = Invoke-Api -Name "管理员产品列表查询(keyword)" -Method "GET" -Path "/api/admin/products?page=1&size=20&keyword=$productName" -Token $adminToken
    $adminProductId = Find-IdByKeyword -Records (Get-Records $adminProductList.data) -Field "title" -Keyword $productName
    if ($adminProductId) {
        Invoke-Api -Name "管理员更新产品" -Method "PUT" -Path "/api/admin/products/$adminProductId" -Token $adminToken -Body @{
            providerId    = $providerUserId
            title         = "$productName-更新"
            description   = "后台新增产品(更新)"
            destinationId = $adminDestId
            duration      = 3
            price         = 459
            originalPrice = 559
            productType   = 0
            tags          = "后台,测试,更新"
            includeItems  = "门票,导游"
            excludeItems  = "餐饮"
            stock         = 40
            sales         = 0
            status        = 1
        } | Out-Null
        Invoke-Api -Name "管理员更新产品状态(下架)" -Method "PUT" -Path "/api/admin/products/$adminProductId/status" -Token $adminToken -Body @{ status = 0 } | Out-Null
        Invoke-Api -Name "管理员删除产品" -Method "DELETE" -Path "/api/admin/products/$adminProductId" -Token $adminToken | Out-Null
    } else {
        Add-Result -Name "管理员产品更新/状态/删除" -Method "PUT|DELETE" -Path "/api/admin/products/{id}*" -Pass $false -Status "SKIP" -BizCode -1 -Message "未获取到后台测试产品ID"
    }

    if ($adminAttrId) { Invoke-Api -Name "管理员删除景点" -Method "DELETE" -Path "/api/admin/attractions/$adminAttrId" -Token $adminToken | Out-Null }
    if ($adminHotelId) { Invoke-Api -Name "管理员删除酒店" -Method "DELETE" -Path "/api/admin/hotels/$adminHotelId" -Token $adminToken | Out-Null }
    if ($adminTransportId) { Invoke-Api -Name "管理员删除交通" -Method "DELETE" -Path "/api/admin/transports/$adminTransportId" -Token $adminToken | Out-Null }
    Invoke-Api -Name "管理员删除目的地" -Method "DELETE" -Path "/api/admin/destinations/$adminDestId" -Token $adminToken | Out-Null
} else {
    Add-Result -Name "管理员资源 CRUD" -Method "POST|PUT|DELETE" -Path "/api/admin/*" -Pass $false -Status "SKIP" -BizCode -1 -Message "未获取到测试目的地ID，跳过资源链路后续操作"
}

# =========================
# 管理员统计与订单管理
# =========================

Write-Step "管理员统计与订单管理"

Invoke-Api -Name "管理员统计概览" -Method "GET" -Path "/api/admin/stats/overview" -Token $adminToken | Out-Null
Invoke-Api -Name "管理员订单趋势统计" -Method "GET" -Path "/api/admin/stats/orders?days=7" -Token $adminToken | Out-Null
Invoke-Api -Name "管理员热门目的地统计" -Method "GET" -Path "/api/admin/stats/hot-destinations?limit=5" -Token $adminToken | Out-Null
Invoke-Api -Name "管理员用户偏好统计" -Method "GET" -Path "/api/admin/stats/user-preferences?limit=5" -Token $adminToken | Out-Null
Invoke-Api -Name "管理员订单列表" -Method "GET" -Path "/api/admin/orders?page=1&size=20&keyword=$Marker" -Token $adminToken | Out-Null

if ($order1Id) {
    Invoke-Api -Name "管理员修改订单状态(幂等验证)" -Method "PUT" -Path "/api/admin/orders/$order1Id/status" -Token $adminToken -Body @{ status = 3 } | Out-Null
} else {
    Add-Result -Name "管理员修改订单状态" -Method "PUT" -Path "/api/admin/orders/{id}/status" -Pass $false -Status "SKIP" -BizCode -1 -Message "未获取订单#1 ID"
}

# =========================
# 结果输出
# =========================

Write-Step "输出测试报告"

if (-not (Test-Path $ReportDir)) {
    New-Item -ItemType Directory -Path $ReportDir -Force | Out-Null
}

$Results | ConvertTo-Json -Depth 20 | Set-Content -Encoding UTF8 -Path $ReportFile

$total = $Results.Count
$pass = ($Results | Where-Object { $_.status -eq "PASS" }).Count
$fail = ($Results | Where-Object { $_.status -eq "FAIL" }).Count
$skip = ($Results | Where-Object { $_.status -eq "SKIP" }).Count

Write-Host ""
Write-Host "测试完成: TOTAL=$total PASS=$pass FAIL=$fail SKIP=$skip" -ForegroundColor White
Write-Host "详细报告: $ReportFile" -ForegroundColor White

if ($fail -gt 0 -and $StrictStop) {
    exit 1
}
