package com.sysml.mvp.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.model.EMFModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.sirius.components.emf.services.EditingContextCrossReferenceAdapter;
import org.eclipse.sirius.emfjson.resource.JsonResource;
import org.eclipse.sirius.emfjson.resource.JsonResourceFactoryImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
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
 * 使用sirius-emfjson库进行JSON序列化
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FileModelRepository {
    
    @Value("${app.data.projects-path:./data/projects}")
    private String dataRoot;
    
    private final EMFModelRegistry modelRegistry;
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
    
    // 允许测试时设置数据目录
    public void setDataRoot(String dataRoot) {
        this.dataRoot = dataRoot;
    }
    
    /**
     * 获取项目文件路径
     */
    private Path getProjectPath(String projectId) {
        return Paths.get(dataRoot, "projects", projectId, "model.json");
    }
    
    /**
     * 创建JsonResource（使用sirius-emfjson）
     */
    private JsonResource createJsonResource(URI uri, ResourceSet resourceSet) {
        // 使用JsonResourceFactoryImpl创建资源
        JsonResourceFactoryImpl factory = new JsonResourceFactoryImpl();
        JsonResource resource = (JsonResource) factory.createResource(uri);
        resourceSet.getResources().add(resource);
        
        // 添加CrossReferenceAdapter处理引用
        resourceSet.eAdapters().add(new EditingContextCrossReferenceAdapter());
        
        return resource;
    }
    
    /**
     * 创建正确配置的ResourceSet
     */
    private ResourceSet createConfiguredResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();
        
        // 注册JsonResourceFactory
        JsonResourceFactoryImpl factory = new JsonResourceFactoryImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("json", factory);
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put("file", factory);
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put("urn", factory);
        
        // 注册本地SysML EPackage
        EPackage sysmlPackage = modelRegistry.getSysmlPackage();
        if (sysmlPackage != null) {
            resourceSet.getPackageRegistry().put(sysmlPackage.getNsURI(), sysmlPackage);
            resourceSet.getPackageRegistry().put("", sysmlPackage);  // 支持fragment-only引用
            log.debug("在ResourceSet中注册SysML EPackage: {}", sysmlPackage.getNsURI());
        }
        
        return resourceSet;
    }
    
    /**
     * 加载项目模型
     */
    public Resource loadProject(String projectId) {
        Path projectPath = getProjectPath(projectId);
        
        // 从缓存获取或创建新的ResourceSet
        ResourceSet resourceSet = resourceCache.computeIfAbsent(projectId, 
            k -> createConfiguredResourceSet());
        
        URI uri = URI.createFileURI(projectPath.toString());
        JsonResource resource = createJsonResource(uri, resourceSet);
        
        if (!Files.exists(projectPath)) {
            // 创建空项目
            log.info("创建新项目: {}", projectId);
            return resource;
        }
        
        try {
            // 加载选项：避免循环引用
            Map<String, Object> options = new HashMap<>();
            options.put(JsonResource.OPTION_ENCODING, "UTF-8");
            options.put(JsonResource.OPTION_FORCE_DEFAULT_REFERENCE_SERIALIZATION, Boolean.TRUE);
            
            resource.load(options);
            log.debug("加载项目: {}, 包含{}个对象", projectId, resource.getContents().size());
            return resource;
        } catch (IOException e) {
            log.error("加载项目失败: {}", projectId, e);
            // 如果加载失败，返回空资源
            return resource;
        }
    }
    
    /**
     * 保存项目模型
     */
    public void saveProject(String projectId, Resource resource) {
        try {
            Path projectPath = getProjectPath(projectId);
            Files.createDirectories(projectPath.getParent());
            
            // 保存选项：生成标准EMF JSON格式
            Map<String, Object> options = new HashMap<>();
            options.put(JsonResource.OPTION_ENCODING, "UTF-8");
            options.put(JsonResource.OPTION_FORCE_DEFAULT_REFERENCE_SERIALIZATION, Boolean.TRUE);
            options.put(JsonResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
            
            resource.save(options);
            
            // 更新时间戳
            updateProjectTimestamp(projectId);
            
            log.debug("保存项目: {}, 包含{}个对象", projectId, resource.getContents().size());
        } catch (IOException e) {
            log.error("保存项目失败: {}", projectId, e);
            throw new RuntimeException("Failed to save project: " + projectId, e);
        }
    }
    
    /**
     * 将资源保存为JSON字符串
     */
    public String saveToString(Resource resource) {
        try {
            // 创建临时资源用于序列化
            ResourceSet tempResourceSet = createConfiguredResourceSet();
            URI tempUri = URI.createURI("temp://export.json");
            JsonResource tempResource = createJsonResource(tempUri, tempResourceSet);
            
            // 复制内容
            tempResource.getContents().addAll(resource.getContents());
            
            // 保存到字节流
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            Map<String, Object> options = new HashMap<>();
            options.put(JsonResource.OPTION_ENCODING, "UTF-8");
            options.put(JsonResource.OPTION_FORCE_DEFAULT_REFERENCE_SERIALIZATION, Boolean.TRUE);
            options.put(JsonResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
            
            tempResource.save(outputStream, options);
            
            return outputStream.toString("UTF-8");
        } catch (Exception e) {
            log.error("资源序列化失败", e);
            throw new RuntimeException("Failed to serialize resource to string", e);
        }
    }
    
    /**
     * 从JSON字符串加载资源
     */
    public Resource loadFromString(String json) {
        try {
            // 创建临时资源用于反序列化
            ResourceSet tempResourceSet = createConfiguredResourceSet();
            URI tempUri = URI.createURI("temp://import.json");
            JsonResource tempResource = createJsonResource(tempUri, tempResourceSet);
            
            // 从字节流加载
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(json.getBytes("UTF-8"));
            Map<String, Object> options = new HashMap<>();
            options.put(JsonResource.OPTION_ENCODING, "UTF-8");
            options.put(JsonResource.OPTION_FORCE_DEFAULT_REFERENCE_SERIALIZATION, Boolean.TRUE);
            
            tempResource.load(inputStream, options);
            
            return tempResource;
        } catch (Exception e) {
            log.error("字符串反序列化失败", e);
            throw new RuntimeException("Failed to deserialize string to resource", e);
        }
    }

    /**
     * 导出项目到指定路径
     */
    public void exportProject(String projectId, Path exportPath) {
        try {
            Resource resource = loadProject(projectId);
            
            // 创建临时ResourceSet用于导出
            ResourceSet exportResourceSet = createConfiguredResourceSet();
            URI exportUri = URI.createFileURI(exportPath.toString());
            JsonResource exportResource = createJsonResource(exportUri, exportResourceSet);
            
            // 复制所有内容
            exportResource.getContents().addAll(resource.getContents());
            
            // 保存
            Map<String, Object> options = new HashMap<>();
            options.put(JsonResource.OPTION_ENCODING, "UTF-8");
            options.put(JsonResource.OPTION_FORCE_DEFAULT_REFERENCE_SERIALIZATION, Boolean.TRUE);
            options.put(JsonResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
            
            exportResource.save(options);
            log.info("导出项目: {} 到 {}", projectId, exportPath);
        } catch (IOException e) {
            log.error("导出项目失败: {} 到 {}", projectId, exportPath, e);
            throw new RuntimeException("Failed to export project: " + projectId, e);
        }
    }
    
    /**
     * 从指定路径导入项目
     */
    public void importProject(String projectId, Path importPath) {
        try {
            // 创建新的ResourceSet用于导入
            ResourceSet importResourceSet = createConfiguredResourceSet();
            URI importUri = URI.createFileURI(importPath.toString());
            JsonResource importResource = createJsonResource(importUri, importResourceSet);
            
            // 加载导入文件
            Map<String, Object> options = new HashMap<>();
            options.put(JsonResource.OPTION_ENCODING, "UTF-8");
            options.put(JsonResource.OPTION_FORCE_DEFAULT_REFERENCE_SERIALIZATION, Boolean.TRUE);
            
            importResource.load(options);
            
            // 创建目标项目资源
            ResourceSet targetResourceSet = createConfiguredResourceSet();
            Path targetPath = getProjectPath(projectId);
            Files.createDirectories(targetPath.getParent());
            
            URI targetUri = URI.createFileURI(targetPath.toString());
            JsonResource targetResource = createJsonResource(targetUri, targetResourceSet);
            
            // 复制内容
            targetResource.getContents().addAll(importResource.getContents());
            
            // 保存到目标位置
            targetResource.save(options);
            
            // 更新缓存
            resourceCache.put(projectId, targetResourceSet);
            
            log.info("导入项目: {} 从 {}", projectId, importPath);
        } catch (IOException e) {
            log.error("导入项目失败: {} 从 {}", projectId, importPath, e);
            throw new RuntimeException("Failed to import project: " + projectId, e);
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
            Path metadataPath = Paths.get(dataRoot, "projects", projectId, "metadata.json");
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
            Path projectsPath = Paths.get(dataRoot, "projects");
            if (!Files.exists(projectsPath)) {
                return Collections.emptyList();
            }
            
            return Files.list(projectsPath)
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