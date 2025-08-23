package com.sysml.mvp.controller;

import com.sysml.mvp.dto.HealthResponse;
import com.sysml.mvp.dto.ModelHealthResponse;
import com.sysml.mvp.model.EMFModelRegistry;
import org.eclipse.emf.ecore.EPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 健康检查控制器
 * REQ-A2-1: 基础健康检查
 * REQ-A2-2: EMF模型健康检查
 */
@Slf4j
@RestController
public class HealthController {
    
    @Value("${app.version:0.1.0-MVP}")
    private String buildVersion;
    
    @Value("${git.commit.id:unknown}")
    private String gitCommit;
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    /**
     * 基础健康检查端点
     * REQ-A2-1: GET /health 响应<100ms
     */
    @GetMapping("/health")
    public HealthResponse health() {
        log.debug("执行健康检查");
        
        return HealthResponse.builder()
            .status("UP")
            .buildVersion(buildVersion)
            .gitCommit(gitCommit)
            .serverTimeUtc(Instant.now().toString())
            .storage("JSON_FILE_SYSTEM")
            .build();
    }
    
    /**
     * EMF模型健康检查
     * REQ-A2-2: 返回已注册EPackage列表
     */
    @GetMapping("/health/model")
    public ModelHealthResponse modelHealth() {
        log.debug("执行模型健康检查");
        
        List<ModelHealthResponse.PackageInfo> packages = new ArrayList<>();
        
        // 获取已注册的包
        EPackage sysmlPackage = modelRegistry.getSysmlPackage();
        if (sysmlPackage != null) {
            packages.add(ModelHealthResponse.PackageInfo.builder()
                .nsUri(sysmlPackage.getNsURI())
                .name(sysmlPackage.getName())
                .source("local")
                .classCount(sysmlPackage.getEClassifiers().size())
                .build());
        }
        
        return ModelHealthResponse.builder()
            .status("UP")
            .packages(packages)
            .dataDirectory("data/projects")
            .projectCount(countProjects())
            .totalElements(countTotalElements())
            .build();
    }
    
    private int countProjects() {
        // TODO: 实现项目计数
        return 0;
    }
    
    private int countTotalElements() {
        // TODO: 实现元素计数
        return 0;
    }
}