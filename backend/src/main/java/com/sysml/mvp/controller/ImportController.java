package com.sysml.mvp.controller;

import com.sysml.mvp.command.ImportDemoDataCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*")
public class ImportController {
    
    @Autowired
    private ImportDemoDataCommand importCommand;
    
    @PostMapping("/import-demo")
    public ResponseEntity<Map<String, Object>> importDemoData() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 直接调用导入逻辑
            importCommand.run("import-demo");
            
            response.put("success", true);
            response.put("message", "Demo数据导入成功");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}