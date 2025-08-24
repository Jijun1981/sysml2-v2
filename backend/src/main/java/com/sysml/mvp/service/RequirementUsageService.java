package com.sysml.mvp.service;

import com.sysml.mvp.dto.RequirementUsageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.ecore.EObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * RequirementUsage服务 - 基于Pilot元模型的动态EMF实现
 * 
 * REQ-C2-1: POST /requirements（type=usage, of=defId）
 * REQ-C2-2: 更新允许name,text,status,tags；存在Trace时删除→409
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementUsageService {
    
    private final PilotEMFService pilotEMFService;
    
    public RequirementUsageDTO createUsage(RequirementUsageDTO dto) {
        log.info("创建RequirementUsage(Pilot): of={}, name={}", dto.getOf(), dto.getName());
        
        // 输入验证
        if (dto.getOf() == null || dto.getOf().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "of(Definition引用)不能为空");
        }
        
        // 使用PilotEMFService创建
        EObject reqUsage = pilotEMFService.createRequirementUsage(
            dto.getOf(), 
            dto.getName(), 
            dto.getText(),
            dto.getStatus() != null ? dto.getStatus() : "draft"
        );
        
        // 转换为DTO返回
        RequirementUsageDTO result = new RequirementUsageDTO();
        result.setId((String) pilotEMFService.getAttributeValue(reqUsage, "elementId"));
        result.setEClass("RequirementUsage");
        result.setType("usage");
        result.setOf(dto.getOf());
        result.setName(dto.getName());
        result.setText(dto.getText());
        result.setStatus(dto.getStatus());
        result.setCreatedAt(Instant.now());
        result.setUpdatedAt(Instant.now());
        
        log.info("成功创建RequirementUsage: id={}", result.getId());
        return result;
    }
    
    public RequirementUsageDTO getUsageById(String id) {
        // TODO: 实现基于ID的RequirementUsage查询
        throw new UnsupportedOperationException("RequirementUsage查询功能待实现");
    }
    
    public RequirementUsageDTO updateUsage(String id, RequirementUsageDTO dto) {
        // TODO: 实现RequirementUsage更新
        throw new UnsupportedOperationException("RequirementUsage更新功能待实现");
    }
    
    public void deleteUsage(String id) {
        // TODO: 实现RequirementUsage删除，检查Trace引用
        throw new UnsupportedOperationException("RequirementUsage删除功能待实现");
    }
}