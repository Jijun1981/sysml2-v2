package com.sysml.mvp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.data.BatterySystemDemoDataGenerator;
import com.sysml.mvp.data.TestDataSetGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Demo数据生成控制器 - REQ-B1-4
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/demo")
public class DemoDataController {
    
    private final BatterySystemDemoDataGenerator batterySystemGenerator;
    private final TestDataSetGenerator testDataSetGenerator;
    private final ObjectMapper objectMapper;
    
    public DemoDataController(BatterySystemDemoDataGenerator batterySystemGenerator, 
                             TestDataSetGenerator testDataSetGenerator,
                             ObjectMapper objectMapper) {
        this.batterySystemGenerator = batterySystemGenerator;
        this.testDataSetGenerator = testDataSetGenerator;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 生成电池系统Demo数据
     */
    @GetMapping("/battery-system")
    public ResponseEntity<Map<String, Object>> generateBatterySystemData() {
        try {
            log.info("生成电池系统Demo数据");
            
            Map<String, Object> demoData = batterySystemGenerator.generateBatterySystemDemoData();
            
            log.info("成功生成电池系统Demo数据，包含{}个元素", 
                ((java.util.List<?>) demoData.get("content")).size());
            
            return ResponseEntity.ok(demoData);
            
        } catch (Exception e) {
            log.error("生成电池系统Demo数据失败", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to generate demo data", "message", e.getMessage()));
        }
    }
    
    /**
     * 生成测试数据集
     */
    @GetMapping("/dataset/{size}")
    public ResponseEntity<Map<String, Object>> generateTestDataSet(@PathVariable String size) {
        try {
            log.info("生成{}测试数据集", size);
            
            TestDataSetGenerator.DataSetSize dataSetSize;
            try {
                dataSetSize = TestDataSetGenerator.DataSetSize.valueOf(size.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid size", "message", "Size must be one of: small, medium, large"));
            }
            
            long startTime = System.currentTimeMillis();
            Map<String, Object> testData = testDataSetGenerator.generateTestDataSet(dataSetSize);
            long endTime = System.currentTimeMillis();
            
            log.info("成功生成{}测试数据集，包含{}个元素，耗时{}ms", 
                size, ((java.util.List<?>) testData.get("content")).size(), endTime - startTime);
            
            return ResponseEntity.ok(testData);
            
        } catch (Exception e) {
            log.error("生成{}测试数据集失败", size, e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to generate test dataset", "message", e.getMessage()));
        }
    }
}