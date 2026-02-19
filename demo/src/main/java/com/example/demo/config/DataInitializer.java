package com.example.demo.config;

import com.example.demo.service.CordaNodeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CordaNodeManager nodeManager;

    @Override
    public void run(String... args) throws Exception {
        // 只有当数据库中没有 partyA 时，才初始化默认数据
        try {
            nodeManager.getNodeService("partyA");
        } catch (IllegalArgumentException e) {
            System.out.println("初始化默认 Corda 节点数据到 PostgreSQL...");
            nodeManager.saveNode("partyA", "http://localhost:50005");
            nodeManager.saveNode("partyB", "http://localhost:50006");
            nodeManager.saveNode("partyC", "http://localhost:50008");
            nodeManager.saveNode("partyE", "http://localhost:50009");
        }
    }
}