package com.sysml.mvp.service;

import com.sysml.mvp.dto.RequirementDefinitionDTO;
import com.sysml.mvp.dto.TraceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 视图服务层
 * 实现REQ-D1-1, REQ-D2-1, REQ-D3-1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewService {
    
    private final RequirementService requirementService;
    private final TraceService traceService;
    
    /**
     * REQ-D1-1: 获取树视图数据
     * AC: GET /tree 返回Definition为父、Usage为子；支持懒加载
     */
    public Map<String, Object> getTreeData() {
        log.debug("获取树视图数据");
        
        // 获取所有需求
        List<RequirementDefinitionDTO> allRequirements = requirementService.listRequirements();
        
        // 分离Definition和Usage
        List<RequirementDefinitionDTO> definitions = allRequirements.stream()
            .filter(r -> "RequirementDefinition".equals(r.getEClass()))
            .collect(Collectors.toList());
        
        List<RequirementDefinitionDTO> usages = allRequirements.stream()
            .filter(r -> "RequirementUsage".equals(r.getEClass()))
            .collect(Collectors.toList());
        
        // 构建树形结构
        Map<String, Object> root = new HashMap<>();
        root.put("id", "root");
        root.put("label", "Requirements");
        
        List<Map<String, Object>> children = new ArrayList<>();
        
        // 为每个Definition创建节点
        for (RequirementDefinitionDTO def : definitions) {
            Map<String, Object> defNode = new HashMap<>();
            defNode.put("id", def.getId());
            defNode.put("label", def.getReqId() + ": " + def.getName());
            defNode.put("type", "definition");
            
            // 查找该Definition的所有Usage
            List<Map<String, Object>> usageChildren = new ArrayList<>();
            for (RequirementDefinitionDTO usage : usages) {
                if (def.getId().equals(usage.getOf())) {
                    Map<String, Object> usageNode = new HashMap<>();
                    usageNode.put("id", usage.getId());
                    usageNode.put("label", usage.getName());
                    usageNode.put("type", "usage");
                    usageNode.put("children", new ArrayList<>());
                    usageChildren.add(usageNode);
                }
            }
            
            defNode.put("children", usageChildren);
            children.add(defNode);
        }
        
        // 添加没有Definition的孤立Usage
        for (RequirementDefinitionDTO usage : usages) {
            if (usage.getOf() == null || usage.getOf().isEmpty()) {
                Map<String, Object> usageNode = new HashMap<>();
                usageNode.put("id", usage.getId());
                usageNode.put("label", usage.getName());
                usageNode.put("type", "usage");
                usageNode.put("children", new ArrayList<>());
                children.add(usageNode);
            }
        }
        
        root.put("children", children);
        
        log.debug("树视图数据构建完成，节点数: {}", children.size());
        return root;
    }
    
    /**
     * REQ-D2-1: 获取表视图数据
     * AC: GET /table?page&size&sort&q，列包含reqId,name,type,tags,status
     */
    public Map<String, Object> getTableData(int page, int size, String sort, String query) {
        log.debug("获取表视图数据: page={}, size={}, sort={}, query={}", page, size, sort, query);
        
        // 验证size参数范围 (1,200]
        if (size < 1 || size > 200) {
            throw new IllegalArgumentException("size参数必须在1到200之间");
        }
        
        // 获取所有需求
        List<RequirementDefinitionDTO> allRequirements = requirementService.listRequirements();
        
        // 过滤（如果有搜索条件）
        if (query != null && !query.isEmpty()) {
            String lowerQuery = query.toLowerCase();
            allRequirements = allRequirements.stream()
                .filter(r -> 
                    (r.getName() != null && r.getName().toLowerCase().contains(lowerQuery)) ||
                    (r.getText() != null && r.getText().toLowerCase().contains(lowerQuery)) ||
                    (r.getReqId() != null && r.getReqId().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());
        }
        
        // 排序（如果有排序条件）
        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split(",");
            String field = sortParts[0];
            boolean ascending = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]);
            
            allRequirements.sort((a, b) -> {
                int result = 0;
                switch (field) {
                    case "reqId":
                        result = compareNullable(a.getReqId(), b.getReqId());
                        break;
                    case "name":
                        result = compareNullable(a.getName(), b.getName());
                        break;
                    case "status":
                        result = compareNullable(a.getStatus(), b.getStatus());
                        break;
                    default:
                        result = 0;
                }
                return ascending ? result : -result;
            });
        }
        
        // 分页
        int totalElements = allRequirements.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        
        List<RequirementDefinitionDTO> pageContent = allRequirements.subList(fromIndex, toIndex);
        
        // 构建表格数据
        Map<String, Object> tableData = new HashMap<>();
        tableData.put("columns", Arrays.asList("reqId", "name", "type", "tags", "status"));
        
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RequirementDefinitionDTO req : pageContent) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", req.getId());
            row.put("reqId", req.getReqId());
            row.put("name", req.getName());
            row.put("type", "RequirementDefinition".equals(req.getEClass()) ? "definition" : "usage");
            row.put("tags", req.getTags() != null ? req.getTags() : new ArrayList<>());
            row.put("status", req.getStatus() != null ? req.getStatus() : "approved");
            rows.add(row);
        }
        
        tableData.put("rows", rows);
        tableData.put("page", page);
        tableData.put("totalPages", totalPages);
        tableData.put("totalElements", totalElements);
        
        log.debug("表视图数据构建完成，行数: {}", rows.size());
        return tableData;
    }
    
    /**
     * REQ-D3-1: 获取图视图数据
     * AC: GET /graph?rootId 返回nodes/edges；节点含id,type,label，边含from,to,type
     */
    public Map<String, Object> getGraphData(String rootId) {
        log.debug("获取图视图数据: rootId={}", rootId);
        
        Map<String, Object> graphData = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        
        // 获取需求和追溯关系
        List<RequirementDefinitionDTO> requirements;
        List<TraceDTO> traces;
        
        if (rootId != null && !rootId.isEmpty()) {
            // 如果指定了rootId，只返回相关的子图
            requirements = new ArrayList<>();
            Set<String> visitedIds = new HashSet<>();
            collectRelatedRequirements(rootId, requirements, visitedIds);
            
            // 获取相关的追溯关系
            traces = traceService.findAllTraces().stream()
                .filter(t -> visitedIds.contains(t.getFromId()) || visitedIds.contains(t.getToId()))
                .collect(Collectors.toList());
        } else {
            // 返回全部数据（可以在这里实现分页）
            requirements = requirementService.listRequirements();
            traces = traceService.findAllTraces();
            
            // 限制最大节点数（MVP性能限制）
            if (requirements.size() > 100) {
                requirements = requirements.subList(0, 100);
            }
        }
        
        // 生成节点
        int x = 100;
        int y = 100;
        for (RequirementDefinitionDTO req : requirements) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", req.getId());
            node.put("type", "requirement");
            node.put("label", req.getReqId() != null ? req.getReqId() : req.getName());
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", x);
            position.put("y", y);
            node.put("position", position);
            
            Map<String, Object> data = new HashMap<>();
            data.put("eClass", req.getEClass());
            data.put("name", req.getName());
            node.put("data", data);
            
            nodes.add(node);
            
            // 简单布局算法
            x += 200;
            if (x > 800) {
                x = 100;
                y += 150;
            }
        }
        
        // 生成边
        for (TraceDTO trace : traces) {
            Map<String, Object> edge = new HashMap<>();
            edge.put("id", trace.getId());
            edge.put("source", trace.getFromId());
            edge.put("target", trace.getToId());
            edge.put("type", trace.getType());
            edge.put("label", trace.getType());
            edges.add(edge);
        }
        
        graphData.put("nodes", nodes);
        graphData.put("edges", edges);
        
        log.debug("图视图数据构建完成，节点数: {}, 边数: {}", nodes.size(), edges.size());
        return graphData;
    }
    
    private void collectRelatedRequirements(String rootId, List<RequirementDefinitionDTO> result, Set<String> visited) {
        if (visited.contains(rootId)) {
            return;
        }
        visited.add(rootId);
        
        // 查找根节点
        RequirementDefinitionDTO root = requirementService.getRequirement(rootId);
        if (root != null) {
            result.add(root);
            
            // 查找相关的追溯关系
            List<TraceDTO> traces = traceService.getTracesByRequirement(rootId, "both");
            for (TraceDTO trace : traces) {
                String relatedId = trace.getFromId().equals(rootId) ? trace.getToId() : trace.getFromId();
                collectRelatedRequirements(relatedId, result, visited);
            }
            
            // 如果是Definition，查找其Usage
            if ("RequirementDefinition".equals(root.getEClass())) {
                List<RequirementDefinitionDTO> allReqs = requirementService.listRequirements();
                for (RequirementDefinitionDTO req : allReqs) {
                    if (rootId.equals(req.getOf())) {
                        collectRelatedRequirements(req.getId(), result, visited);
                    }
                }
            }
            // 如果是Usage，查找其Definition
            else if ("RequirementUsage".equals(root.getEClass()) && root.getOf() != null) {
                collectRelatedRequirements(root.getOf(), result, visited);
            }
        }
    }
    
    private int compareNullable(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }
}