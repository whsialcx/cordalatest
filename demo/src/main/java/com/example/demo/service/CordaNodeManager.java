package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// import com.example.demo.config.CordaConfig;

import java.util.Map;

@Service
public class CordaNodeManager {
    
    @Autowired
    private Map<String, CordaService> nodeServices;
    
    // @Autowired
    // private CordaConfig cordaConfig;
    
    /**
     * 获取指定节点的服务
     * @param nodeName 节点名称 (partyA, partyB, partyC, partyE)
     * @return CordaService 实例
     */
    public CordaService getNodeService(String nodeName) {
        CordaService service = nodeServices.get(nodeName);
        if (service == null) {
            throw new IllegalArgumentException("未找到节点: " + nodeName);
        }
        return service;
    }
    
    /**
     * 获取所有节点服务
     * @return 节点名称到服务的映射
     */
    public Map<String, CordaService> getAllNodeServices() {
        return nodeServices;
    }
    
    /**
     * 获取默认节点服务（节点A）
     * @return CordaService 实例
     */
    public CordaService getDefaultNodeService() {
        return getNodeService("partyA");
    }
    
    /**
     * 获取所有节点名称
     * @return 节点名称列表
     */
    public String[] getNodeNames() {
        return nodeServices.keySet().toArray(new String[0]);
    }
}