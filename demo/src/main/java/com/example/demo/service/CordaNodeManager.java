package com.example.demo.service;

import com.example.demo.entity.CordaNode;
import com.example.demo.repository.CordaNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CordaNodeManager {

    @Autowired
    private CordaNodeRepository nodeRepository;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 获取指定节点的服务 (动态实例化)
     */
    public CordaService getNodeService(String nodeName) {
        CordaNode node = nodeRepository.findByName(nodeName)
                .orElseThrow(() -> new IllegalArgumentException("未找到节点: " + nodeName));
        
        // 使用数据库中存储的 baseUrl 动态创建 CordaService
        return new CordaService(restTemplate, node.getBaseUrl());
    }

    /**
     * 获取所有节点名称
     */
    public String[] getNodeNames() {
        return nodeRepository.findAll().stream()
                .map(CordaNode::getName)
                .toArray(String[]::new);
    }

    public List<CordaNode> getAllNodeDetails() {
        return nodeRepository.findAll();
    }

    /**
     * 获取默认节点服务（如果有这个需求，可以指定一个默认的逻辑）
     */
    public CordaService getDefaultNodeService() {
        return getNodeService("partyA"); 
    }

    // --- 新增：用于在添加/删除节点时同步更新数据库 ---

    @Transactional
    public void saveNode(String name, String baseUrl) {
        CordaNode node = nodeRepository.findByName(name).orElse(new CordaNode());
        node.setName(name);
        node.setBaseUrl(baseUrl);
        nodeRepository.save(node);
    }

    @Transactional
    public void removeNode(String name) {
        nodeRepository.deleteByName(name);
    }
}