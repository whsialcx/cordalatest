package com.example.demo.controller;

import com.example.demo.service.PowerShellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/nodes")// 所有接口以/api/nodes开头
public class NodeController 
{
    @Autowired
    private PowerShellService powerShellService;
    
    //验证 Corda 项目配置
    @GetMapping("/validate")
    public Map<String, Object> validateCordaProject() 
    {
        Map<String, Object> response = new HashMap<>();//构建响应
        
        PowerShellService.CordaProjectInfo projectInfo = powerShellService.getCordaProjectInfo();
        
        response.put("projectRoot", projectInfo.getProjectRoot());// 项目根目录
        response.put("rootExists", projectInfo.isRootExists());
        response.put("buildGradleExists", projectInfo.isBuildGradleExists());//build.gradle是否存在
        response.put("scriptExists", projectInfo.isScriptExists());//脚本是否存在
        response.put("scriptPath", projectInfo.getScriptPath());
        response.put("valid", projectInfo.isRootExists() && projectInfo.isBuildGradleExists() && projectInfo.isScriptExists());
        
        return response;
    }

    @GetMapping("/list")//显示所有节点
    public Map<String, Object> listNodes() 
    {
        Map<String, Object> response = new HashMap<>();
        try 
        {
            if (!powerShellService.validateCordaProject()) 
            {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }
            java.util.List<String> nodes = powerShellService.getNodeNames();
            response.put("success", true);
            response.put("nodes", nodes);
            response.put("count", nodes.size());
        } 
        catch (Exception e) 
        {
            response.put("success", false);
            response.put("message", "获取节点列表时发生错误: " + e.getMessage());
        }
        return response;
    }

    
    @PostMapping("/add")
    public Map<String, Object> addNode(@RequestBody NodeRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 先验证 Corda 项目配置
            if (!powerShellService.validateCordaProject()) {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                PowerShellService.CordaProjectInfo info = powerShellService.getCordaProjectInfo();
                response.put("projectInfo", info);
                return response;
            }
            
            // 修复：使用双引号构建参数
            StringBuilder arguments = new StringBuilder();
            arguments.append("-NodeName \"").append(request.getNodeName()).append("\"");
            
            if (request.isAutoPorts()) {
                arguments.append(" -AutoPorts");
            } else {
                if (request.getP2pPort() != null) 
                    arguments.append(" -P2PPort ").append(request.getP2pPort());
                if (request.getRpcPort() != null) 
                    arguments.append(" -RPCPort ").append(request.getRpcPort());
                if (request.getAdminPort() != null) 
                    arguments.append(" -AdminPort ").append(request.getAdminPort());
            }
            
            if (request.isAutoDb()) {
                arguments.append(" -AutoDb");
            } else {
                if (request.getDbName() != null) 
                    arguments.append(" -DbName \"").append(request.getDbName()).append("\"");
                if (request.getDbUser() != null) 
                    arguments.append(" -DbUser \"").append(request.getDbUser()).append("\"");
            }
            
            // 执行脚本
            PowerShellService.ProcessResult result = 
                powerShellService.executePowerShellScript(arguments.toString());
            
            // 其余代码保持不变...
            if (result.isSuccess() && result.getExitCode() == 0) {
                response.put("success", true);
                response.put("message", "节点添加成功");
                response.put("output", result.getOutput());
            } else {
                response.put("success", false);
                response.put("message", "节点添加失败");
                response.put("error", result.getError());
                response.put("output", result.getOutput());
                response.put("exitCode", result.getExitCode());
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "执行脚本时发生错误: " + e.getMessage());
        }
        
        return response;
    }
    // 用于构造网络
    @PostMapping("/deploy")
    public Map<String, Object> deployNetwork() 
    {
        Map<String, Object> response = new HashMap<>();
        try 
        {
            if (!powerShellService.validateCordaProject()) 
            {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }

            PowerShellService.ProcessResult result = powerShellService.executeGradleDeploy();

            if (result.isSuccess() && result.getExitCode() == 0) 
            {
                response.put("success", true);
                response.put("message", "gradlew deployNodes 执行成功");
                response.put("output", result.getOutput());
            } 
            else
            {
                response.put("success", false);
                response.put("message", "gradlew deployNodes 执行失败");
                response.put("exitCode", result.getExitCode());
                response.put("output", result.getOutput());
                response.put("error", result.getError());
            }
        } 
        catch (Exception e) 
        {
            response.put("success", false);
            response.put("message", "执行部署时发生错误: " + e.getMessage());
        }
        return response;
    }
    // 删除节点
    @PostMapping("/remove")
    public Map<String, Object> removeNode(@RequestBody RemoveNodeRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!powerShellService.validateCordaProject()) {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }
            
            // 修复：使用双引号构建参数，确保特殊字符正确处理
            String arguments = "-RemoveNode \"" + request.getNodeName() + "\"";
            
            // 执行
            PowerShellService.ProcessResult result = 
                powerShellService.executePowerShellScript(arguments);
            
            if (result.isSuccess() && result.getExitCode() == 0) {
                response.put("success", true);
                response.put("message", "节点删除成功");
                response.put("output", result.getOutput());
            } else {
                response.put("success", false);
                response.put("message", "节点删除失败");
                response.put("error", result.getError());
                response.put("output", result.getOutput());
                response.put("exitCode", result.getExitCode());
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "执行脚本时发生错误: " + e.getMessage());
        }
        
        return response;
    }

    @PostMapping("/start-all")//启动所有节点
    public Map<String, Object> startAllNodes() 
    {
        Map<String, Object> response = new HashMap<>();
        try 
        {
            if (!powerShellService.validateCordaProject()) 
            {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }

            // 执行 runnodes.bat脚本来启动所有节点
            PowerShellService.ProcessResult result = powerShellService.executeRunnodesScript();

            if (result.isSuccess() && result.getExitCode() == 0) {
                response.put("success", true);
                response.put("message", "节点启动成功");
                response.put("output", result.getOutput());
            } 
            else 
            {
                response.put("success", false);
                response.put("message", "节点启动失败");
                response.put("exitCode", result.getExitCode());
                response.put("output", result.getOutput());
                response.put("error", result.getError());
            }
        } 
        catch (Exception e) 
        {
            response.put("success", false);
            response.put("message", "执行启动脚本时发生错误: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/start")//启动某一个节点
    public Map<String, Object> startNode(@RequestBody StartNodeRequest request) 
    {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!powerShellService.validateCordaProject()) 
            {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }

            PowerShellService.ProcessResult result = powerShellService.startNode(request.getNodeName());

            if (result.isSuccess() && result.getExitCode() == 0) 
            {
                response.put("success", true);
                response.put("message", "节点启动成功");
                response.put("output", result.getOutput());
            } 
            else 
            {
                response.put("success", false);
                response.put("message", "节点启动失败");
                response.put("exitCode", result.getExitCode());
                response.put("output", result.getOutput());
                response.put("error", result.getError());
            }
        }
        catch (Exception e) 
        {
            response.put("success", false);
            response.put("message", "执行启动脚本时发生错误: " + e.getMessage());
        }
        return response;
    }

    @PostMapping("/stop")//终止节点
    public Map<String, Object> stopNode(@RequestBody StartNodeRequest request) 
    {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!powerShellService.validateCordaProject()) 
            {
                response.put("success", false);
                response.put("message", "Corda 项目配置验证失败，请检查配置");
                return response;
            }

            PowerShellService.ProcessResult result = powerShellService.stopNode(request.getNodeName());

            if (result.isSuccess() && result.getExitCode() == 0) {
                response.put("success", true);
                response.put("message", "节点停止成功");
                response.put("output", result.getOutput());
            } 
            else 
            {
                response.put("success", false);
                response.put("message", "节点停止失败");
                response.put("exitCode", result.getExitCode());
                response.put("output", result.getOutput());
                response.put("error", result.getError());
            }
        } 
        catch (Exception e) 
        {
            response.put("success", false);
            response.put("message", "停止节点时发生错误: " + e.getMessage());
        }
        return response;
    }
    
    public static class StartNodeRequest 
    {
        private String nodeName;

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }
    }
        
    
    // 请求 DTO 类
    public static class NodeRequest {
        private String nodeName;
        private Integer p2pPort;
        private Integer rpcPort;
        private Integer adminPort;
        private String dbName;
        private String dbUser;
        private boolean autoPorts = false;
        private boolean autoDb = false;
        
        // Getters and Setters
        public String getNodeName() { return nodeName; }
        public void setNodeName(String nodeName) { this.nodeName = nodeName; }
        
        public Integer getP2pPort() { return p2pPort; }
        public void setP2pPort(Integer p2pPort) { this.p2pPort = p2pPort; }
        
        public Integer getRpcPort() { return rpcPort; }
        public void setRpcPort(Integer rpcPort) { this.rpcPort = rpcPort; }
        
        public Integer getAdminPort() { return adminPort; }
        public void setAdminPort(Integer adminPort) { this.adminPort = adminPort; }
        
        public String getDbName() { return dbName; }
        public void setDbName(String dbName) { this.dbName = dbName; }
        
        public String getDbUser() { return dbUser; }
        public void setDbUser(String dbUser) { this.dbUser = dbUser; }
        
        public boolean isAutoPorts() { return autoPorts; }
        public void setAutoPorts(boolean autoPorts) { this.autoPorts = autoPorts; }
        
        public boolean isAutoDb() { return autoDb; }
        public void setAutoDb(boolean autoDb) { this.autoDb = autoDb; }
    }
    
    public static class RemoveNodeRequest {
        private String nodeName;
        
        public String getNodeName() { return nodeName; }
        public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    }
}