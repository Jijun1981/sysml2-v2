package com.sysml.mvp.controller;

import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.service.UniversalElementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 高级查询控制器 - 第四阶段数据增强层
 * 
 * 实现功能：
 * - 分页支持：page从0开始，size∈(1..200]，默认50
 * - 排序支持：sort参数，支持多字段排序
 * - 过滤支持：filter参数，支持字段过滤
 * - 全文搜索：search参数
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/elements")
public class AdvancedQueryController {
    
    private final UniversalElementService universalElementService;
    
    // 支持排序的字段白名单
    private static final Set<String> SORTABLE_FIELDS = Set.of(
        "elementId", "declaredName", "eClass", "createdAt", "updatedAt", "reqId", "text"
    );
    
    // 支持过滤的字段白名单
    private static final Set<String> FILTERABLE_FIELDS = Set.of(
        "eClass", "status", "reqId", "declaredName"
    );
    
    public AdvancedQueryController(UniversalElementService universalElementService) {
        this.universalElementService = universalElementService;
    }
    
    /**
     * 高级查询接口 - 支持分页、排序、过滤、搜索
     */
    @GetMapping("/advanced")
    public ResponseEntity<?> advancedQuery(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) List<String> sort,
            @RequestParam(required = false) List<String> filter,
            @RequestParam(required = false) String search) {
        try {
            log.info("高级查询请求: page={}, size={}, sort={}, filter={}, search={}", 
                page, size, sort, filter, search);
            
            // 详细调试参数
            if (sort != null) {
                log.info("Sort参数详情: size={}, content={}", sort.size(), sort);
                for (int i = 0; i < sort.size(); i++) {
                    log.info("Sort[{}]: '{}'", i, sort.get(i));
                }
            }
            
            // 参数验证
            Map<String, String> validationErrors = validateParameters(page, size, sort, filter);
            if (!validationErrors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bad Request",
                    "message", validationErrors.values().iterator().next()
                ));
            }
            
            // 获取所有元素
            List<ElementDTO> allElements = universalElementService.getAllElements();
            
            // 应用过滤
            List<ElementDTO> filteredElements = applyFilters(allElements, filter);
            
            // 应用搜索
            List<ElementDTO> searchedElements = applySearch(filteredElements, search);
            
            // 应用排序
            List<ElementDTO> sortedElements = applySort(searchedElements, sort);
            
            // 应用分页
            PagedResult pagedResult = applyPagination(sortedElements, page, size);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("content", pagedResult.content);
            response.put("page", page);
            response.put("size", size);
            response.put("totalElements", pagedResult.totalElements);
            response.put("totalPages", pagedResult.totalPages);
            response.put("first", page == 0);
            response.put("last", page >= pagedResult.totalPages - 1);
            
            // 添加查询参数到响应中用于验证
            if (sort != null && !sort.isEmpty()) {
                response.put("sort", parseSortParameters(sort));
            }
            if (filter != null && !filter.isEmpty()) {
                response.put("filter", parseFilterParameters(filter));
            }
            if (search != null) {
                response.put("search", search);
            }
            
            long resultCount = pagedResult.content.size();
            log.info("高级查询完成: 返回{}个元素，总计{}个元素", resultCount, pagedResult.totalElements);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("高级查询失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal Server Error",
                "message", "Advanced query failed: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 验证查询参数
     */
    private Map<String, String> validateParameters(int page, int size, List<String> sort, List<String> filter) {
        Map<String, String> errors = new HashMap<>();
        
        // 验证page
        if (page < 0) {
            errors.put("page", "page must be >= 0");
        }
        
        // 验证size
        if (size < 1 || size > 200) {
            errors.put("size", "size must be between 1 and 200");
        }
        
        // 验证sort格式和字段
        if (sort != null) {
            // Spring Boot会按逗号分割参数，所以需要处理两种情况：
            // 1. sort=["field", "direction"] (单个排序参数被分割)
            // 2. sort=["field,direction"] (多个排序参数的情况)
            
            if (sort.size() == 2 && !sort.get(0).contains(",") && !sort.get(1).contains(",")) {
                // 情况1：单个排序参数被分割为两个元素
                String field = sort.get(0);
                String direction = sort.get(1);
                
                if (!SORTABLE_FIELDS.contains(field)) {
                    errors.put("sort", "Invalid sort field: " + field);
                } else if (!direction.equals("asc") && !direction.equals("desc")) {
                    errors.put("sort", "Invalid sort direction: " + direction + ". Use: asc or desc");
                }
            } else {
                // 情况2：每个元素应该是完整的"field,direction"格式
                for (String sortParam : sort) {
                    String[] parts = sortParam.split(",");
                    if (parts.length != 2) {
                        errors.put("sort", "Invalid sort format. Use: field,direction");
                        break;
                    }
                    String field = parts[0];
                    String direction = parts[1];
                    
                    if (!SORTABLE_FIELDS.contains(field)) {
                        errors.put("sort", "Invalid sort field: " + field);
                        break;
                    }
                    
                    if (!direction.equals("asc") && !direction.equals("desc")) {
                        errors.put("sort", "Invalid sort direction: " + direction + ". Use: asc or desc");
                        break;
                    }
                }
            }
        }
        
        // 验证filter格式和字段
        if (filter != null) {
            for (String filterParam : filter) {
                String[] parts = filterParam.split(":");
                if (parts.length != 2) {
                    errors.put("filter", "Invalid filter format. Use: field:value");
                    break;
                }
                String field = parts[0];
                
                if (!FILTERABLE_FIELDS.contains(field)) {
                    errors.put("filter", "Invalid filter field: " + field);
                    break;
                }
            }
        }
        
        return errors;
    }
    
    /**
     * 应用过滤条件
     */
    private List<ElementDTO> applyFilters(List<ElementDTO> elements, List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return elements;
        }
        
        Map<String, String> filterMap = parseFilterParameters(filters);
        
        return elements.stream()
            .filter(element -> {
                for (Map.Entry<String, String> entry : filterMap.entrySet()) {
                    String field = entry.getKey();
                    String value = entry.getValue();
                    
                    switch (field) {
                        case "eClass":
                            if (!element.getEClass().equals(value) && !element.getEClass().equals("sysml:" + value)) {
                                return false;
                            }
                            break;
                        case "status":
                            Map<String, Object> props = element.getProperties();
                            if (props == null || !value.equals(props.get("status"))) {
                                return false;
                            }
                            break;
                        case "reqId":
                            props = element.getProperties();
                            if (props == null || !value.equals(props.get("reqId"))) {
                                return false;
                            }
                            break;
                        case "declaredName":
                            props = element.getProperties();
                            if (props == null || props.get("declaredName") == null || 
                                !props.get("declaredName").toString().contains(value)) {
                                return false;
                            }
                            break;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 应用搜索条件
     */
    private List<ElementDTO> applySearch(List<ElementDTO> elements, String search) {
        if (search == null || search.trim().isEmpty()) {
            return elements;
        }
        
        String searchLower = search.toLowerCase();
        
        return elements.stream()
            .filter(element -> {
                // 搜索elementId
                if (element.getElementId() != null && element.getElementId().toLowerCase().contains(searchLower)) {
                    return true;
                }
                
                // 搜索declaredName
                Map<String, Object> props = element.getProperties();
                if (props != null && props.get("declaredName") != null && 
                    props.get("declaredName").toString().toLowerCase().contains(searchLower)) {
                    return true;
                }
                
                // 搜索属性中的其他文本字段
                if (props != null) {
                    for (Object value : props.values()) {
                        if (value != null && value.toString().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                    }
                }
                
                return false;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 应用排序
     */
    private List<ElementDTO> applySort(List<ElementDTO> elements, List<String> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return elements;
        }
        
        Map<String, String> sortMap = parseSortParameters(sorts);
        
        return elements.stream()
            .sorted((e1, e2) -> {
                for (Map.Entry<String, String> entry : sortMap.entrySet()) {
                    String field = entry.getKey();
                    String direction = entry.getValue();
                    
                    int comparison = compareElements(e1, e2, field);
                    if (comparison != 0) {
                        return direction.equals("desc") ? -comparison : comparison;
                    }
                }
                return 0;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 比较元素的指定字段
     */
    private int compareElements(ElementDTO e1, ElementDTO e2, String field) {
        switch (field) {
            case "elementId":
                return compareNullable(e1.getElementId(), e2.getElementId());
            case "declaredName":
                return comparePropertyValue(e1, e2, "declaredName");
            case "eClass":
                return compareNullable(e1.getEClass(), e2.getEClass());
            case "createdAt":
            case "updatedAt":
            case "reqId":
            case "text":
                return comparePropertyValue(e1, e2, field);
            default:
                return 0;
        }
    }
    
    /**
     * 比较属性值
     */
    private int comparePropertyValue(ElementDTO e1, ElementDTO e2, String property) {
        Map<String, Object> props1 = e1.getProperties();
        Map<String, Object> props2 = e2.getProperties();
        
        Object value1 = props1 != null ? props1.get(property) : null;
        Object value2 = props2 != null ? props2.get(property) : null;
        
        if (value1 == null && value2 == null) return 0;
        if (value1 == null) return -1;
        if (value2 == null) return 1;
        
        return value1.toString().compareTo(value2.toString());
    }
    
    /**
     * 安全比较可能为null的字符串
     */
    private int compareNullable(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        return s1.compareTo(s2);
    }
    
    /**
     * 应用分页
     */
    private PagedResult applyPagination(List<ElementDTO> elements, int page, int size) {
        int totalElements = elements.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        
        List<ElementDTO> content = (start < totalElements) ? 
            elements.subList(start, end) : new ArrayList<>();
        
        return new PagedResult(content, totalElements, totalPages);
    }
    
    /**
     * 解析排序参数
     */
    private Map<String, String> parseSortParameters(List<String> sorts) {
        Map<String, String> sortMap = new LinkedHashMap<>();
        
        if (sorts.size() == 2 && !sorts.get(0).contains(",") && !sorts.get(1).contains(",")) {
            // 情况1：单个排序参数被Spring分割为两个元素
            sortMap.put(sorts.get(0), sorts.get(1));
        } else {
            // 情况2：多个排序参数，每个都是"field,direction"格式
            for (String sort : sorts) {
                String[] parts = sort.split(",");
                if (parts.length == 2) {
                    sortMap.put(parts[0], parts[1]);
                }
            }
        }
        
        return sortMap;
    }
    
    /**
     * 解析过滤参数
     */
    private Map<String, String> parseFilterParameters(List<String> filters) {
        Map<String, String> filterMap = new HashMap<>();
        for (String filter : filters) {
            String[] parts = filter.split(":");
            if (parts.length == 2) {
                filterMap.put(parts[0], parts[1]);
            }
        }
        return filterMap;
    }
    
    /**
     * 分页结果内部类
     */
    private static class PagedResult {
        public final List<ElementDTO> content;
        public final int totalElements;
        public final int totalPages;
        
        public PagedResult(List<ElementDTO> content, int totalElements, int totalPages) {
            this.content = content;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }
    }
}