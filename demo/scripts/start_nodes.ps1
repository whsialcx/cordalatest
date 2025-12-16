[CmdletBinding(DefaultParameterSetName="Help")]
param(
    [Parameter(ParameterSetName="All")]
    [switch]$All,
    
    [Parameter(ParameterSetName="Multiple")]
    [string[]]$Nodes,
    
    [Parameter(ParameterSetName="Single")]
    [string]$Node,
    
    [Parameter(ParameterSetName="Help")]
    [switch]$Help,
    
    [Parameter()]
    [switch]$Background,
    
    [Parameter()]
    [switch]$ListAvailable,
    
    [Parameter()]
    [int]$StartDelay = 10
)

# --- 颜色定义 ---
$ESC = [char]27
$RED = "$ESC[91m"
$GREEN = "$ESC[92m"
$YELLOW = "$ESC[93m"
$BLUE = "$ESC[94m"
$CYAN = "$ESC[96m"
$MAGENTA = "$ESC[95m"
$NC = "$ESC[0m"

# --- 日志函数 ---
function Write-Info { param([string]$message) Write-Host "${BLUE}[INFO]${NC} $message" }
function Write-Success { param([string]$message) Write-Host "${GREEN}[SUCCESS]${NC} $message" }
function Write-Warning { param([string]$message) Write-Host "${YELLOW}[WARNING]${NC} $message" }
function Write-Error { param([string]$message) Write-Host "${RED}[ERROR]${NC} $message" }
function Write-Debug { param([string]$message) Write-Host "${CYAN}[DEBUG]${NC} $message" }

function Show-Usage {
    Write-Host "${MAGENTA}节点服务启动脚本${NC}"
    Write-Host "用法: start_nodes.ps1 [选项]"
    Write-Host ""
    Write-Host "启动模式:"
    Write-Host "  -All                 启动所有可用的节点服务"
    Write-Host "  -Node <节点名>        启动单个节点服务 (例如: PartyA, PartyB)"
    Write-Host "  -Nodes <节点列表>     启动多个节点服务 (例如: PartyA,PartyB,PartyC)"
    Write-Host ""
    Write-Host "其他选项:"
    Write-Host "  -Background          在后台运行服务 (不打开新窗口)"
    Write-Host "  -ListAvailable       列出所有可用的节点服务"
    Write-Host "  -StartDelay <秒>     服务启动间隔 (默认: 10秒)"
    Write-Host "  -Help                显示此帮助信息"
    Write-Host ""
    Write-Host "示例:"
    Write-Host "  # 启动所有节点服务"
    Write-Host "  .\start_nodes.ps1 -All"
    Write-Host ""
    Write-Host "  # 启动单个节点服务"
    Write-Host "  .\start_nodes.ps1 -Node PartyA"
    Write-Host ""
    Write-Host "  # 启动多个节点服务"
    Write-Host "  .\start_nodes.ps1 -Nodes PartyA,PartyB,PartyC"
    Write-Host ""
    Write-Host "  # 启动所有服务并在后台运行"
    Write-Host "  .\start_nodes.ps1 -All -Background"
    Write-Host ""
    Write-Host "  # 列出所有可用节点服务"
    Write-Host "  .\start_nodes.ps1 -ListAvailable"
    Write-Host ""
}

# 获取所有可用的节点服务任务
function Get-AvailableNodeTasks {
    $clientsBuildFile = "clients/build.gradle"
    if (-not (Test-Path $clientsBuildFile)) {
        Write-Error "找不到 $clientsBuildFile 文件"
        return @()
    }
    
    $content = Get-Content $clientsBuildFile -Raw -Encoding UTF8
    
    # 查找所有 runPartyXServer 任务
    $pattern = 'task (runParty\w+Server)\(type: JavaExec, dependsOn: assemble\)'
    $matches = [regex]::Matches($content, $pattern)
    
    $tasks = @()
    foreach ($match in $matches) {
        $taskName = $match.Groups[1].Value
        $nodeName = $taskName -replace '^run|Server$', ''
        $tasks += @{
            TaskName = $taskName
            NodeName = $nodeName
        }
    }
    
    return $tasks
}

# 提取节点服务器端口信息
function Get-NodeServerPort {
    param([string]$TaskName)
    
    $clientsBuildFile = "clients/build.gradle"
    if (-not (Test-Path $clientsBuildFile)) {
        return $null
    }
    
    $content = Get-Content $clientsBuildFile -Raw -Encoding UTF8
    
    # 查找任务定义并提取端口
    $pattern = "(?s)task $TaskName\(type: JavaExec, dependsOn: assemble\) \{.*?args '--server\.port=(\d+)'.*?\}"
    
    if ($content -match $pattern) {
        return $matches[1]
    }
    
    return $null
}

# 列出所有可用节点服务
function Show-AvailableNodes {
    Write-Info "正在获取可用的节点服务..."
    
    $tasks = Get-AvailableNodeTasks
    
    if ($tasks.Count -eq 0) {
        Write-Warning "没有找到可用的节点服务"
        return
    }
    
    Write-Host "${GREEN}可用的节点服务:${NC}" -ForegroundColor Green
    Write-Host ""
    Write-Host "序号 | 节点名称 | 任务名称 | 服务器端口"
    Write-Host "----|----------|----------|------------"
    
    $index = 1
    foreach ($task in $tasks) {
        $port = Get-NodeServerPort -TaskName $task.TaskName
        Write-Host "$index".PadRight(5) + "| $($task.NodeName.PadRight(10)) | $($task.TaskName.PadRight(12)) | $(if($port){$port}else{'N/A'})"
        $index++
    }
    
    Write-Host ""
    Write-Info "共找到 $($tasks.Count) 个节点服务"
}

# 验证节点是否存在
function Test-NodeTaskExists {
    param([string]$NodeName)
    
    $tasks = Get-AvailableNodeTasks
    $taskName = "run${NodeName}Server"
    
    foreach ($task in $tasks) {
        if ($task.TaskName -eq $taskName) {
            return $true
        }
    }
    
    return $false
}

# 启动单个节点服务
function Start-NodeService {
    param(
        [string]$NodeName,
        [switch]$Background,
        [int]$Delay = 0
    )
    
    $taskName = "run${NodeName}Server"
    
    Write-Info "正在启动节点服务: $NodeName (任务: $taskName)"
    
    # 检查任务是否存在
    if (-not (Test-NodeTaskExists -NodeName $NodeName)) {
        Write-Error "节点服务 '$NodeName' 不存在！"
        return $false
    }
    
    if ($Delay -gt 0) {
        Write-Info "等待 $Delay 秒后启动..."
        Start-Sleep -Seconds $Delay
    }
    
    try {
        if ($Background) {
            # 在后台运行
            Write-Debug "在后台运行: gradlew.bat $taskName"
            Start-Process -FilePath "gradlew.bat" -ArgumentList $taskName -NoNewWindow -RedirectStandardOutput "logs/$NodeName.log" -RedirectStandardError "logs/$NodeName-error.log" -PassThru
            Write-Success "节点服务 '$NodeName' 已在后台启动 (日志输出到 logs/$NodeName.log)"
        } else {
            # 在新窗口中运行 - 修复 && 语法问题
            Write-Debug "在新窗口中运行: gradlew.bat $taskName"
            $cmdScript = @"
title Corda Node: $NodeName
gradlew.bat $taskName
pause
"@
            # 创建临时批处理文件
            $tempBatFile = "temp_$NodeName.bat"
            $cmdScript | Out-File -FilePath $tempBatFile -Encoding ASCII
            Start-Process -FilePath "cmd.exe" -ArgumentList "/c", $tempBatFile -NoNewWindow:$false
            # 清理临时文件（延迟清理）
            Start-Job -ScriptBlock {
                Start-Sleep -Seconds 2
                if (Test-Path $using:tempBatFile) {
                    Remove-Item $using:tempBatFile -Force
                }
            }
            Write-Success "节点服务 '$NodeName' 已在新窗口中启动"
        }
        return $true
    } catch {
        Write-Error "启动节点服务 '$NodeName' 时出错: $($_.Exception.Message)"
        return $false
    }
}

# 启动所有节点服务
function Start-AllNodeServices {
    param(
        [switch]$Background,
        [int]$StartDelay = 10
    )
    
    $tasks = Get-AvailableNodeTasks
    
    if ($tasks.Count -eq 0) {
        Write-Error "没有找到可用的节点服务"
        return
    }
    
    Write-Info "正在启动所有 $($tasks.Count) 个节点服务..."
    
    $startedCount = 0
    $failedCount = 0
    
    # 创建日志目录
    if (-not (Test-Path "logs")) {
        New-Item -ItemType Directory -Path "logs" -Force | Out-Null
    }
    
    foreach ($task in $tasks) {
        $result = Start-NodeService -NodeName $task.NodeName -Background:$Background -Delay $StartDelay
        
        if ($result) {
            $startedCount++
        } else {
            $failedCount++
        }
    }
    
    Write-Host ""
    if ($startedCount -gt 0) {
        Write-Success "成功启动了 $startedCount 个节点服务"
    }
    if ($failedCount -gt 0) {
        Write-Warning "有 $failedCount 个节点服务启动失败"
    }
    
    if ($Background) {
        Write-Info "所有节点服务都在后台运行中，日志文件保存在 logs/ 目录下"
    } else {
        Write-Info "每个节点服务都在独立的窗口中运行"
    }
}

# 启动多个指定节点服务
function Start-MultipleNodeServices {
    param(
        [string[]]$NodeNames,
        [switch]$Background,
        [int]$StartDelay = 10
    )
    
    Write-Info "正在启动指定的 $($NodeNames.Count) 个节点服务: $($NodeNames -join ', ')"
    
    $startedCount = 0
    $failedCount = 0
    $invalidNodes = @()
    
    # 验证所有节点是否存在
    foreach ($node in $NodeNames) {
        if (-not (Test-NodeTaskExists -NodeName $node)) {
            $invalidNodes += $node
        }
    }
    
    if ($invalidNodes.Count -gt 0) {
        Write-Warning "以下节点服务不存在: $($invalidNodes -join ', ')"
        
        # 询问是否继续启动其他节点
        $choice = Read-Host "是否继续启动其他存在的节点? (Y/N)"
        if ($choice -notmatch '^[Yy]') {
            Write-Info "操作已取消"
            return
        }
        
        # 过滤掉不存在的节点
        $NodeNames = $NodeNames | Where-Object { $invalidNodes -notcontains $_ }
        
        if ($NodeNames.Count -eq 0) {
            Write-Error "没有有效的节点可以启动"
            return
        }
    }
    
    # 创建日志目录
    if (-not (Test-Path "logs")) {
        New-Item -ItemType Directory -Path "logs" -Force | Out-Null
    }
    
    foreach ($node in $NodeNames) {
        $result = Start-NodeService -NodeName $node -Background:$Background -Delay $StartDelay
        
        if ($result) {
            $startedCount++
        } else {
            $failedCount++
        }
    }
    
    Write-Host ""
    if ($startedCount -gt 0) {
        Write-Success "成功启动了 $startedCount 个节点服务"
    }
    if ($failedCount -gt 0) {
        Write-Warning "有 $failedCount 个节点服务启动失败"
    }
}

# --- 主逻辑 ---
function Main {
    if ($PSScriptRoot) {
        # PowerShell 3.0+ 推荐使用 $PSScriptRoot
        $scriptDir = $PSScriptRoot
    } elseif ($MyInvocation.MyCommand.Path) {
        # 备用方法
        $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    } else {
        # 如果都无法获取，使用当前目录
        Write-Warning "无法确定脚本目录，使用当前目录"
        $scriptDir = Get-Location
    }
    
    # 记录当前工作目录
    $currentDir = Get-Location
    
    # 只有在确实需要时才切换目录
    if ($scriptDir -ne $currentDir.Path) {
        Write-Info "切换工作目录到: $scriptDir"
        Set-Location $scriptDir
    } else {
        Write-Info "当前工作目录: $scriptDir"
    }
    
    # 检查是否显示帮助
    if ($Help) {
        Show-Usage
        return
    }
    
    # 检查是否列出可用节点
    if ($ListAvailable) {
        Show-AvailableNodes
        return
    }
    
    # 根据参数集执行相应操作
    switch ($PSCmdlet.ParameterSetName) {
        "All" {
            Start-AllNodeServices -Background:$Background -StartDelay $StartDelay
        }
        "Single" {
            if (-not $Node) {
                Write-Error "必须指定要启动的节点名称"
                Show-Usage
                return
            }
            
            Start-NodeService -NodeName $Node -Background:$Background
        }
        "Multiple" {
            if ($Nodes.Count -eq 0) {
                Write-Error "必须指定要启动的节点列表"
                Show-Usage
                return
            }
            
            Start-MultipleNodeServices -NodeNames $Nodes -Background:$Background -StartDelay $StartDelay
        }
        "Help" {
            Show-Usage
        }
        default {
            # 如果没有指定参数，显示可用节点并让用户选择
            Write-Info "未指定启动模式，显示可用节点..."
            Show-AvailableNodes
            
            Write-Host ""
            Write-Info "请使用以下方式启动节点:"
            Write-Host "  .\start_nodes.ps1 -Node PartyA"
            Write-Host "  .\start_nodes.ps1 -Nodes PartyA,PartyB,PartyC"
            Write-Host "  .\start_nodes.ps1 -All"
            Write-Host "  .\start_nodes.ps1 -All -Background"
        }
    }
}

# 执行主函数
Main

# 在新窗口中启动所有节点
# .\start_nodes.ps1 -All

# 在后台启动所有节点（日志保存到 logs/ 目录）
# .\start_nodes.ps1 -All -Background

# 启动所有节点，每个间隔3秒
# .\start_nodes.ps1 -All -StartDelay 3

# 4. 列出所有可用节点：
# .\start_nodes.ps1 -ListAvailable

# 启动 PartyA, PartyB, PartyC
# .\start_nodes.ps1 -Nodes PartyA,PartyB,PartyC