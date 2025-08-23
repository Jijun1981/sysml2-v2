package com.sysml.mvp.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 文件系统模型仓库
 * 使用JSON文件存储EMF模型
 */
@Slf4j
@Repository
public class FileModelRepository {
    
    @Value("${app.data.projects-path:./data/projects}")
    private String dataRoot;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, ResourceSet> resourceCache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() throws IOException {
        // 确保数据目录存在
        Path dataPath = Paths.get(dataRoot);
        if (!Files.exists(dataPath)) {
            Files.createDirectories(dataPath);
            log.info("创建数据目录: {}", dataPath);
        }
    }
    
    /**
     * 获取项目文件路径
     */
    private Path getProjectPath(String projectId) {
        return Paths.get(dataRoot, projectId, "model.json");
    }
    
    /**
     * 加载项目模型
     */
    public Resource loadProject(String projectId) {
        Path projectPath = getProjectPath(projectId);
        
        // 如果文件不存在，创建空项目
        if (!Files.exists(projectPath)) {
            return createEmptyProject(projectId);
        }
        
        // 从缓存获取或创建新的ResourceSet
        ResourceSet resourceSet = resourceCache.computeIfAbsent(projectId, 
            k -> new ResourceSetImpl());
        
        // 创建Resource
        Resource resource = resourceSet.createResource(
            URI.createFileURI(projectPath.toString()));
        
        try {
            // 加载文件
            resource.load(Collections.emptyMap());
            log.debug("加载项目: {}", projectId);
            return resource;
        } catch (IOException e) {
            log.error("加载项目失败: {}", projectId, e);
            throw new RuntimeException("Failed to load project: " + projectId, e);
        }
    }
    
    /**
     * 保存项目模型
     */
    public void saveProject(String projectId, Resource resource) {
        try {
            Path projectPath = getProjectPath(projectId);
            Files.createDirectories(projectPath.getParent());
            
            // 保存到文件
            resource.save(Collections.emptyMap());
            
            // 更新时间戳
            updateProjectTimestamp(projectId);
            
            log.debug("保存项目: {}", projectId);
        } catch (IOException e) {
            log.error("保存项目失败: {}", projectId, e);
            throw new RuntimeException("Failed to save project: " + projectId, e);
        }
    }
    
    /**
     * 创建空项目
     */
    private Resource createEmptyProject(String projectId) {
        try {
            Path projectPath = getProjectPath(projectId);
            Files.createDirectories(projectPath.getParent());
            
            ResourceSet resourceSet = new ResourceSetImpl();
            Resource resource = resourceSet.createResource(
                URI.createFileURI(projectPath.toString()));
            
            // 添加到缓存
            resourceCache.put(projectId, resourceSet);
            
            // 保存空文件
            resource.save(Collections.emptyMap());
            
            log.info("创建新项目: {}", projectId);
            return resource;
        } catch (IOException e) {
            log.error("创建项目失败: {}", projectId, e);
            throw new RuntimeException("Failed to create project: " + projectId, e);
        }
    }
    
    /**
     * 按类型查找对象
     */
    public List<EObject> findByType(String projectId, String eClassName) {
        Resource resource = loadProject(projectId);
        return resource.getContents().stream()
            .filter(obj -> eClassName.equals(obj.eClass().getName()))
            .collect(Collectors.toList());
    }
    
    /**
     * 按ID查找对象
     */
    public EObject findById(String projectId, String id) {
        Resource resource = loadProject(projectId);
        return resource.getContents().stream()
            .filter(obj -> {
                // 获取id属性
                var idFeature = obj.eClass().getEStructuralFeature("id");
                if (idFeature != null) {
                    Object value = obj.eGet(idFeature);
                    return id.equals(value);
                }
                return false;
            })
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 更新项目时间戳
     */
    private void updateProjectTimestamp(String projectId) {
        try {
            Path metadataPath = Paths.get(dataRoot, projectId, "metadata.json");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("projectId", projectId);
            metadata.put("updatedAt", Instant.now().toString());
            
            String json = objectMapper.writeValueAsString(metadata);
            Files.writeString(metadataPath, json);
        } catch (IOException e) {
            log.warn("更新时间戳失败: {}", projectId, e);
        }
    }
    
    /**
     * 列出所有项目
     */
    public List<String> listProjects() {
        try {
            Path dataPath = Paths.get(dataRoot);
            if (!Files.exists(dataPath)) {
                return Collections.emptyList();
            }
            
            return Files.list(dataPath)
                .filter(Files::isDirectory)
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("列出项目失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 清除缓存
     */
    public void clearCache(String projectId) {
        resourceCache.remove(projectId);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        resourceCache.clear();
    }
}