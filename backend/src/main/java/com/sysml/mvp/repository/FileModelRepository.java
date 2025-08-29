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
import org.eclipse.sirius.emfjson.resource.JsonResource;
import org.eclipse.sirius.emfjson.resource.JsonResourceFactoryImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
// import java.util.concurrent.ConcurrentHashMap; // 移除，不再使用缓存
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
    
    // 添加EMF引用助手（可选注入）
    @Autowired(required = false)
    private com.sysml.mvp.util.EMFReferenceHelper referenceHelper;
    // 移除缓存机制，确保每次都加载最新数据
    // private final Map<String, ResourceSet> resourceCache = new ConcurrentHashMap<>();
    
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
        // dataRoot已经包含了projects路径，不需要再添加
        return Paths.get(dataRoot, projectId, "model.json");
    }
    
    /**
     * 创建JsonResource（使用sirius-emfjson）
     */
    private JsonResource createJsonResource(URI uri, ResourceSet resourceSet) {
        try {
            // 使用JsonResourceFactoryImpl创建资源
            JsonResourceFactoryImpl factory = new JsonResourceFactoryImpl();
            JsonResource resource = (JsonResource) factory.createResource(uri);
            
            // 确保资源有URI
            if (resource.getURI() == null) {
                resource.setURI(uri);
            }
            
            // 添加到ResourceSet BEFORE任何操作
            if (!resourceSet.getResources().contains(resource)) {
                resourceSet.getResources().add(resource);
            }
            
            // 确保SysML EPackage在ResourceSet中可见
            ensureSysMLPackageInResourceSet(resourceSet);
            
            log.debug("创建JsonResource成功: URI={}, ResourceSet包含{}个资源", 
                uri, resourceSet.getResources().size());
            
            return resource;
        } catch (Exception e) {
            log.error("创建JsonResource失败: URI={}", uri, e);
            throw new RuntimeException("Failed to create JsonResource: " + uri, e);
        }
    }
    
    /**
     * 确保SysML EPackage在ResourceSet中可见，解决GsonEObjectDeserializer.getEPackage null问题
     */
    private void ensureSysMLPackageInResourceSet(ResourceSet resourceSet) {
        try {
            // 创建一个虚拟资源包含SysML EPackage
            URI sysmlUri = URI.createURI("https://www.omg.org/spec/SysML/20250201");
            Resource sysmlResource = resourceSet.getResource(sysmlUri, false);
            
            if (sysmlResource == null) {
                // 创建虚拟资源持有SysML EPackage
                sysmlResource = resourceSet.createResource(sysmlUri);
                // 注意：不需要实际内容，只需要URI存在于ResourceSet中
                log.debug("为ResourceSet添加SysML EPackage虚拟资源: {}", sysmlUri);
            }
        } catch (Exception e) {
            log.warn("无法添加SysML EPackage虚拟资源，但这可能不影响功能", e);
        }
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
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
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
        
        // 每次都创建新的ResourceSet，确保加载最新数据
        ResourceSet resourceSet = createConfiguredResourceSet();
        
        // 使用绝对路径创建URI
        URI uri = URI.createFileURI(projectPath.toAbsolutePath().toString());
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
            
            // 【REQ-TDD-001】后处理：添加derived引用到JSON
            if (referenceHelper != null) {
                referenceHelper.postProcessJsonFile(projectPath, resource);
                log.debug("已执行引用后处理");
            }
            
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
            
            // 缓存已移除，不再需要更新
            // resourceCache.put(projectId, targetResourceSet);
            
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
     * 清除缓存 - 缓存已移除，此方法保留为空实现以保持兼容性
     */
    public void clearCache(String projectId) {
        // 缓存已移除，无需操作
        log.debug("缓存机制已移除，无需清除缓存: {}", projectId);
    }
    
    /**
     * 清除所有缓存 - 缓存已移除，此方法保留为空实现以保持兼容性
     */
    public void clearAllCache() {
        // 缓存已移除，无需操作
        log.debug("缓存机制已移除，无需清除缓存");
    }
}