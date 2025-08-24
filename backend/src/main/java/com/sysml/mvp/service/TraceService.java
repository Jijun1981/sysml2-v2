package com.sysml.mvp.service;

import com.sysml.mvp.dto.TraceDTO;
import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.repository.FileModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Trace服务层
 * 实现REQ-C3-1到REQ-C3-4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceService {
    
    private final FileModelRepository repository;
    private final EMFModelRegistry modelRegistry;
    private static final String PROJECT_ID = "default"; // MVP版本使用固定项目ID
    
    /**
     * REQ-C3-1: 创建Trace关系
     * POST /requirements/{id}/traces {toId,type}
     * type∈{derive,satisfy,refine,trace}；fromId==toId→400；toId不存在→404；成功对象含createdAt(UTC)
     */
    public TraceDTO createTrace(String fromId, TraceDTO.CreateRequest request) {
        log.info("创建Trace关系: fromId={}, toId={}, type={}", fromId, request.getToId(), request.getType());
        
        // 验证fromId==toId的情况 - REQ-C3-1
        if (fromId.equals(request.getToId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromId和toId不能相同");
        }
        
        // 验证fromId存在
        validateRequirementExists(fromId, "fromId对应的需求不存在");
        
        // 验证toId存在 - REQ-C3-1
        validateRequirementExists(request.getToId(), "toId对应的需求不存在");
        
        // 加载资源
        Resource resource = repository.loadProject(PROJECT_ID);
        
        // 使用modelRegistry工厂方法创建Trace对象
        EObject trace = modelRegistry.createTrace(fromId, request.getToId(), request.getType());
        
        // 添加到资源
        resource.getContents().add(trace);
        
        // 保存
        repository.saveProject(PROJECT_ID, resource);
        
        String generatedId = (String) getEObjectAttribute(trace, "id");
        log.info("Trace关系创建成功: id={}", generatedId);
        
        return convertToDTO(trace);
    }
    
    /**
     * REQ-C3-2: 获取需求的Trace关系
     * GET /requirements/{id}/traces?dir=in|out|both 返回入/出边
     */
    public List<TraceDTO> getTracesByRequirement(String requirementId, String direction) {
        log.debug("获取需求的Trace关系: requirementId={}, direction={}", requirementId, direction);
        
        // 验证需求存在
        validateRequirementExists(requirementId, "需求不存在");
        
        // 获取所有Trace
        List<EObject> allTraces = repository.findByType(PROJECT_ID, "Trace");
        
        List<TraceDTO> result = new ArrayList<>();
        
        for (EObject trace : allTraces) {
            String fromId = (String) getEObjectAttribute(trace, "fromId");
            String toId = (String) getEObjectAttribute(trace, "toId");
            
            boolean shouldInclude = false;
            
            if ("in".equals(direction)) {
                // 入边：当前需求作为toId
                shouldInclude = requirementId.equals(toId);
            } else if ("out".equals(direction)) {
                // 出边：当前需求作为fromId
                shouldInclude = requirementId.equals(fromId);
            } else {
                // both或null：包含所有相关的
                shouldInclude = requirementId.equals(fromId) || requirementId.equals(toId);
            }
            
            if (shouldInclude) {
                result.add(convertToDTO(trace));
            }
        }
        
        log.debug("找到{}个Trace关系", result.size());
        return result;
    }
    
    /**
     * 获取所有Trace关系
     * 用于前端初始化加载
     */
    public List<TraceDTO> listAllTraces() {
        log.debug("获取所有Trace关系");
        
        // 获取所有Trace
        List<EObject> allTraces = repository.findByType(PROJECT_ID, "Trace");
        
        List<TraceDTO> result = new ArrayList<>();
        for (EObject trace : allTraces) {
            result.add(convertToDTO(trace));
        }
        
        log.debug("找到{}个Trace关系", result.size());
        return result;
    }
    
    /**
     * 获取单个Trace关系
     * GET /traces/{traceId}→200；不存在→404
     */
    public TraceDTO getTrace(String traceId) {
        log.info("获取Trace关系: traceId={}", traceId);
        
        // 加载资源
        Resource resource = repository.loadProject(PROJECT_ID);
        
        // 查找Trace对象
        EObject trace = resource.getContents().stream()
            .filter(obj -> "Trace".equals(obj.eClass().getName()))
            .filter(obj -> {
                var idFeature = obj.eClass().getEStructuralFeature("id");
                if (idFeature != null) {
                    Object value = obj.eGet(idFeature);
                    return traceId.equals(value);
                }
                return false;
            })
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Trace关系不存在: " + traceId));
        
        return convertToDTO(trace);
    }
    
    /**
     * REQ-C3-4: 删除Trace关系
     * DELETE /traces/{traceId}→204；不存在→404
     */
    public void deleteTrace(String traceId) {
        log.info("删除Trace关系: traceId={}", traceId);
        
        // 加载资源
        Resource resource = repository.loadProject(PROJECT_ID);
        
        // 查找Trace对象
        EObject trace = resource.getContents().stream()
            .filter(obj -> "Trace".equals(obj.eClass().getName()))
            .filter(obj -> {
                var idFeature = obj.eClass().getEStructuralFeature("id");
                if (idFeature != null) {
                    Object value = obj.eGet(idFeature);
                    return traceId.equals(value);
                }
                return false;
            })
            .findFirst()
            .orElse(null);
        
        if (trace == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trace关系不存在: " + traceId);
        }
        
        // 从资源中移除
        boolean removed = resource.getContents().remove(trace);
        log.debug("从资源中移除Trace对象: removed={}, 剩余对象数={}", removed, resource.getContents().size());
        
        // 保存
        repository.saveProject(PROJECT_ID, resource);
        
        log.info("Trace关系删除成功: traceId={}", traceId);
    }
    
    // 私有辅助方法
    
    /**
     * 验证需求是否存在
     */
    private void validateRequirementExists(String requirementId, String errorMessage) {
        EObject requirement = repository.findById(PROJECT_ID, requirementId);
        if (requirement == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage + ": " + requirementId);
        }
        
        String className = requirement.eClass().getName();
        if (!"RequirementDefinition".equals(className) && !"RequirementUsage".equals(className)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage + ": " + requirementId);
        }
    }
    
    /**
     * REQ-C3-3: 查找是否已存在相同的(from,to,type)组合
     */
    public TraceDTO findExistingTrace(String fromId, String toId, String type) {
        List<EObject> traces = repository.findByType(PROJECT_ID, "Trace");
        
        for (EObject trace : traces) {
            String existingFromId = (String) getEObjectAttribute(trace, "fromId");
            String existingToId = (String) getEObjectAttribute(trace, "toId");
            String existingType = (String) getEObjectAttribute(trace, "type");
            
            if (fromId.equals(existingFromId) && 
                toId.equals(existingToId) && 
                type.equals(existingType)) {
                return convertToDTO(trace);
            }
        }
        
        return null;
    }
    
    /**
     * 将EObject转换为TraceDTO
     */
    private TraceDTO convertToDTO(EObject trace) {
        return TraceDTO.builder()
                .id((String) getEObjectAttribute(trace, "id"))
                .fromId((String) getEObjectAttribute(trace, "fromId"))
                .toId((String) getEObjectAttribute(trace, "toId"))
                .type((String) getEObjectAttribute(trace, "type"))
                .createdAt(convertToInstant((Date) getEObjectAttribute(trace, "createdAt")))
                .build();
    }
    
    private Object getEObjectAttribute(EObject object, String attributeName) {
        EStructuralFeature feature = object.eClass().getEStructuralFeature(attributeName);
        return feature != null ? object.eGet(feature) : null;
    }
    
    private Instant convertToInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }
    
    /**
     * 获取所有Trace列表 - for GraphView
     */
    public List<TraceDTO> findAllTraces() {
        log.debug("获取所有Trace列表");
        
        List<EObject> traces = repository.findByType(PROJECT_ID, "Trace");
        return traces.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}