package com.sysml.mvp.controller;

import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.dto.ValidationResultDTO;
import com.sysml.mvp.service.ValidationService;
import com.sysml.mvp.service.UniversalElementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 验证控制器
 * 
 * 需求实现：
 * - REQ-A3-1: 验证API - 完整的REST API端点
 * - REQ-E1-1: MVP规则集 - 仅检测3条核心规则
 * - REQ-E1-2: 规则码固定枚举 - DUP_REQID, CYCLE_DERIVE_REFINE, BROKEN_REF
 * - REQ-E1-3: 验证结果API格式 - 返回ValidationResultDTO
 * - REQ-C1-1: reqId唯一性验证 - 支持reqId重复检测
 * - REQ-C3-3: 追溯关系去重检测 - 支持追溯关系重复验证
 * - REQ-C3-4: 追溯关系语义约束验证 - 支持语义约束检查
 * 
 * 设计说明：
 * 1. 提供静态验证接口，执行3条核心验证规则
 * 2. 提供独立的reqId唯一性验证接口
 * 3. 提供追溯关系验证接口（重复性和语义约束）
 * 4. 提供综合验证接口，组合多种验证场景
 * 5. 标准HTTP状态码和JSON响应格式
 */
@RestController
@RequestMapping("/api/v1/validation")
public class ValidationController {
    
    private final ValidationService validationService;
    private final UniversalElementService universalElementService;
    
    public ValidationController(ValidationService validationService, UniversalElementService universalElementService) {
        this.validationService = validationService;
        this.universalElementService = universalElementService;
    }
    
    /**
     * 【REQ-E1-3】静态验证
     * 对整个模型执行3条核心验证规则
     * @return 200 OK 和验证结果DTO
     */
    @PostMapping("/static")
    public ResponseEntity<ValidationResultDTO> validateStatic() {
        // 获取所有元素
        List<ElementDTO> allElements = universalElementService.getAllElements();
        
        // 执行静态验证
        ValidationResultDTO result = validationService.validateStatic(allElements);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 【REQ-C1-1】reqId唯一性验证
     * @param reqId 需要验证的reqId
     * @return 200 OK 和验证结果
     */
    @GetMapping("/reqId/{reqId}")
    public ResponseEntity<Map<String, Object>> validateReqIdUniqueness(@PathVariable String reqId) {
        boolean isUnique = validationService.validateReqIdUniqueness(reqId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("reqId", reqId);
        response.put("isUnique", isUnique);
        response.put("message", isUnique ? "reqId is available" : "reqId already exists");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 【REQ-C3-3】追溯关系去重验证
     * @param source 源端元素ID
     * @param target 目标端元素ID
     * @param type 追溯类型
     * @return 200 OK 和验证结果，或400 Bad Request如果参数缺失
     */
    @GetMapping("/trace/duplication")
    public ResponseEntity<Map<String, Object>> validateTraceDuplication(
            @RequestParam String source,
            @RequestParam String target,
            @RequestParam String type) {
        
        boolean isDuplicate = !validationService.validateTraceDuplication(source, target, type);
        
        Map<String, Object> response = new HashMap<>();
        response.put("source", source);
        response.put("target", target);
        response.put("type", type);
        response.put("isDuplicate", isDuplicate);
        response.put("message", isDuplicate ? "Trace relationship already exists" : "Trace relationship is unique");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 【REQ-C3-4】追溯关系语义约束验证
     * @param source 源端元素ID
     * @param target 目标端元素ID
     * @param type 追溯类型
     * @return 200 OK 和验证结果，或400 Bad Request如果参数缺失
     */
    @GetMapping("/trace/semantics")
    public ResponseEntity<?> validateTraceSemantics(
            @RequestParam(required = true) String source,
            @RequestParam(required = true) String target,
            @RequestParam(required = true) String type) {
        
        boolean isValid = validationService.validateTraceSemantics(source, target, type);
        String validationMessage = validationService.getTraceSemanticValidationMessage(source, target, type);
        
        Map<String, Object> response = new HashMap<>();
        response.put("source", source);
        response.put("target", target);
        response.put("type", type);
        response.put("isValid", isValid);
        response.put("message", validationMessage);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 【REQ-A3-1】综合验证
     * 组合reqId唯一性和追溯关系验证
     * @param request 包含reqId、source、target、type的请求体
     * @return 200 OK 和综合验证结果
     */
    @PostMapping("/comprehensive")
    public ResponseEntity<Map<String, Object>> comprehensiveValidation(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        // reqId验证
        if (request.containsKey("reqId")) {
            String reqId = request.get("reqId").toString();
            boolean isReqIdUnique = validationService.validateReqIdUniqueness(reqId);
            
            Map<String, Object> reqIdValidation = new HashMap<>();
            reqIdValidation.put("reqId", reqId);
            reqIdValidation.put("isUnique", isReqIdUnique);
            reqIdValidation.put("message", isReqIdUnique ? "reqId is available" : "reqId already exists");
            response.put("reqIdValidation", reqIdValidation);
        }
        
        // 追溯关系验证
        if (request.containsKey("source") && request.containsKey("target") && request.containsKey("type")) {
            String source = request.get("source").toString();
            String target = request.get("target").toString();
            String type = request.get("type").toString();
            
            boolean isDuplicate = !validationService.validateTraceDuplication(source, target, type);
            boolean isSemanticValid = validationService.validateTraceSemantics(source, target, type);
            String validationMessage = validationService.getTraceSemanticValidationMessage(source, target, type);
            
            Map<String, Object> traceValidation = new HashMap<>();
            traceValidation.put("source", source);
            traceValidation.put("target", target);
            traceValidation.put("type", type);
            traceValidation.put("isDuplicate", isDuplicate);
            traceValidation.put("isSemanticValid", isSemanticValid);
            traceValidation.put("validationMessage", validationMessage);
            response.put("traceValidation", traceValidation);
        }
        
        // 计算综合有效性
        boolean overallValid = true;
        if (response.containsKey("reqIdValidation")) {
            Map<String, Object> reqIdValidation = (Map<String, Object>) response.get("reqIdValidation");
            overallValid &= (Boolean) reqIdValidation.get("isUnique");
        }
        if (response.containsKey("traceValidation")) {
            Map<String, Object> traceValidation = (Map<String, Object>) response.get("traceValidation");
            overallValid &= !(Boolean) traceValidation.get("isDuplicate");
            overallValid &= (Boolean) traceValidation.get("isSemanticValid");
        }
        
        response.put("overallValid", overallValid);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 【REQ-E1-1】获取支持的验证规则
     * @return 200 OK 和规则列表
     */
    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> getSupportedRules() {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> dupReqId = new HashMap<>();
        dupReqId.put("code", "DUP_REQID");
        dupReqId.put("description", "Detects duplicate reqId in RequirementDefinition elements");
        dupReqId.put("severity", "ERROR");
        
        Map<String, Object> cycleDeriveRefine = new HashMap<>();
        cycleDeriveRefine.put("code", "CYCLE_DERIVE_REFINE");
        cycleDeriveRefine.put("description", "Detects circular dependencies in derive/refine chains");
        cycleDeriveRefine.put("severity", "ERROR");
        
        Map<String, Object> brokenRef = new HashMap<>();
        brokenRef.put("code", "BROKEN_REF");
        brokenRef.put("description", "Detects references to non-existent elements in trace relationships");
        brokenRef.put("severity", "ERROR");
        
        response.put("rules", java.util.Arrays.asList(dupReqId, cycleDeriveRefine, brokenRef));
        response.put("version", "1.0");
        response.put("totalRules", 3);
        
        return ResponseEntity.ok(response);
    }
}