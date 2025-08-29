// 生成简单的需求数据：6-7个模板，20-30个需求条目

const data = {
    "ns": {
        "sysml": "https://www.omg.org/spec/SysML/20250201"
    },
    "schemaLocation": {
        "https://www.omg.org/spec/SysML/20250201": "file:/mnt/d/sysml2%20v2/backend/../opensource/SysML-v2-Pilot-Implementation/org.omg.sysml/model/SysML.ecore#/-1"
    },
    "json": {
        "encoding": "UTF-8",
        "version": "1.0"
    },
    "content": []
};

// 6个需求定义模板
const templates = [
    {
        id: "DEF-PERF",
        name: "性能需求模板", 
        shortName: "Performance",
        text: "系统性能相关需求的标准模板，用于定义响应时间、吞吐量等性能指标"
    },
    {
        id: "DEF-SEC", 
        name: "安全需求模板",
        shortName: "Security", 
        text: "系统安全相关需求的标准模板，用于定义认证、授权、加密等安全措施"
    },
    {
        id: "DEF-UI",
        name: "界面需求模板",
        shortName: "UI/UX",
        text: "用户界面和用户体验需求的标准模板，用于定义易用性、可访问性等要求"
    },
    {
        id: "DEF-FUNC",
        name: "功能需求模板", 
        shortName: "Functional",
        text: "系统功能性需求的标准模板，用于定义系统应该具备的基本功能"
    },
    {
        id: "DEF-DATA",
        name: "数据需求模板",
        shortName: "Data",
        text: "数据管理需求的标准模板，用于定义数据存储、备份、同步等要求"
    },
    {
        id: "DEF-API",
        name: "接口需求模板",
        shortName: "Interface", 
        text: "系统接口需求的标准模板，用于定义API规范、数据格式等技术接口要求"
    }
];

// 添加需求定义
templates.forEach(template => {
    data.content.push({
        "eClass": "sysml:RequirementDefinition",
        "data": {
            "elementId": template.id,
            "declaredName": template.name,
            "declaredShortName": template.shortName,
            "text": template.text,
            "reqId": template.id,
            "createdAt": "2025-08-29T10:00:00Z",
            "updatedAt": "2025-08-29T10:00:00Z"
        }
    });
});

// 25个需求条目（基于模板）
const requirements = [
    // 性能需求条目
    {
        id: "REQ-001", definitionId: "DEF-PERF", 
        name: "API响应时间", shortName: "API-Response",
        text: "所有REST API接口的响应时间必须在200ms以内", status: "implemented"
    },
    {
        id: "REQ-002", definitionId: "DEF-PERF",
        name: "并发用户数", shortName: "Concurrent-Users", 
        text: "系统必须支持至少1000个并发用户同时访问", status: "verified"
    },
    {
        id: "REQ-003", definitionId: "DEF-PERF",
        name: "页面加载速度", shortName: "Page-Load",
        text: "首页加载时间不超过3秒，其他页面不超过2秒", status: "approved"
    },
    {
        id: "REQ-004", definitionId: "DEF-PERF", 
        name: "数据库查询性能", shortName: "DB-Query",
        text: "复杂查询响应时间不超过1秒", status: "draft"
    },
    
    // 安全需求条目  
    {
        id: "REQ-005", definitionId: "DEF-SEC",
        name: "用户认证", shortName: "User-Auth",
        text: "系统必须实现基于JWT的用户认证机制", status: "implemented"
    },
    {
        id: "REQ-006", definitionId: "DEF-SEC",
        name: "密码策略", shortName: "Password-Policy", 
        text: "用户密码必须至少8位，包含大小写字母和数字", status: "approved"
    },
    {
        id: "REQ-007", definitionId: "DEF-SEC",
        name: "数据加密", shortName: "Data-Encryption",
        text: "敏感数据必须使用AES-256加密存储", status: "verified"
    },
    {
        id: "REQ-008", definitionId: "DEF-SEC",
        name: "访问日志", shortName: "Access-Log", 
        text: "记录所有用户访问和操作日志", status: "implemented"
    },
    
    // 界面需求条目
    {
        id: "REQ-009", definitionId: "DEF-UI",
        name: "响应式设计", shortName: "Responsive",
        text: "界面必须支持PC、平板和手机等多种设备", status: "implemented"
    },
    {
        id: "REQ-010", definitionId: "DEF-UI", 
        name: "多语言支持", shortName: "Multi-Language",
        text: "系统必须支持中文和英文两种语言", status: "approved"
    },
    {
        id: "REQ-011", definitionId: "DEF-UI",
        name: "键盘快捷键", shortName: "Keyboard-Shortcuts",
        text: "常用操作必须提供键盘快捷键支持", status: "draft"
    },
    {
        id: "REQ-012", definitionId: "DEF-UI",
        name: "无障碍访问", shortName: "Accessibility", 
        text: "符合WCAG 2.1 AA级无障碍标准", status: "approved"
    },
    
    // 功能需求条目
    {
        id: "REQ-013", definitionId: "DEF-FUNC",
        name: "用户注册", shortName: "User-Registration",
        text: "提供用户自助注册功能，支持邮箱验证", status: "implemented"
    },
    {
        id: "REQ-014", definitionId: "DEF-FUNC",
        name: "数据导出", shortName: "Data-Export", 
        text: "用户可以导出数据为Excel或CSV格式", status: "verified"
    },
    {
        id: "REQ-015", definitionId: "DEF-FUNC",
        name: "搜索功能", shortName: "Search",
        text: "提供全文搜索功能，支持关键词高亮", status: "approved"
    },
    {
        id: "REQ-016", definitionId: "DEF-FUNC",
        name: "批量操作", shortName: "Batch-Ops",
        text: "支持批量删除、批量修改等操作", status: "draft"
    },
    {
        id: "REQ-017", definitionId: "DEF-FUNC",
        name: "通知中心", shortName: "Notification",
        text: "统一的消息通知中心，支持站内信和邮件", status: "implemented"
    },
    
    // 数据需求条目
    {
        id: "REQ-018", definitionId: "DEF-DATA", 
        name: "数据备份", shortName: "Data-Backup",
        text: "系统必须每天自动备份数据，保留30天", status: "implemented"
    },
    {
        id: "REQ-019", definitionId: "DEF-DATA",
        name: "数据恢复", shortName: "Data-Recovery",
        text: "支持从备份中恢复数据，RTO不超过4小时", status: "approved"
    },
    {
        id: "REQ-020", definitionId: "DEF-DATA",
        name: "数据同步", shortName: "Data-Sync", 
        text: "多节点部署时数据实时同步", status: "draft"
    },
    {
        id: "REQ-021", definitionId: "DEF-DATA",
        name: "数据归档", shortName: "Data-Archive",
        text: "超过一年的历史数据自动归档", status: "approved"
    },
    
    // 接口需求条目
    {
        id: "REQ-022", definitionId: "DEF-API",
        name: "RESTful API", shortName: "RESTful",
        text: "所有接口必须遵循RESTful设计规范", status: "implemented"
    },
    {
        id: "REQ-023", definitionId: "DEF-API", 
        name: "API版本管理", shortName: "API-Version",
        text: "API必须支持版本管理，URL中包含版本号", status: "approved"
    },
    {
        id: "REQ-024", definitionId: "DEF-API",
        name: "数据格式", shortName: "Data-Format",
        text: "接口数据交换格式统一使用JSON", status: "verified"
    },
    {
        id: "REQ-025", definitionId: "DEF-API",
        name: "API文档", shortName: "API-Doc",
        text: "提供OpenAPI 3.0规范的API文档", status: "draft"
    }
];

// 添加需求条目
requirements.forEach((req, index) => {
    data.content.push({
        "eClass": "sysml:RequirementUsage", 
        "data": {
            "elementId": req.id,
            "declaredName": req.name,
            "declaredShortName": req.shortName,
            "text": req.text,
            "requirementDefinition": req.definitionId,
            "reqId": req.id,
            "status": req.status,
            "createdAt": `2025-08-29T${10 + Math.floor(index/10)}:${10 + (index%10)*2}:00Z`,
            "updatedAt": `2025-08-29T${10 + Math.floor(index/10)}:${10 + (index%10)*2}:00Z`
        }
    });
});

// 添加几个依赖关系
const dependencies = [
    {from: "REQ-001", to: "REQ-002", type: "refine"},
    {from: "REQ-005", to: "REQ-006", type: "derive"},
    {from: "REQ-013", to: "REQ-014", type: "trace"},
    {from: "REQ-022", to: "REQ-023", type: "satisfy"}
];

// 依赖关系类型映射到具体EClass
const typeToEClass = {
    "derive": "sysml:DeriveRequirement",
    "satisfy": "sysml:Satisfy",
    "refine": "sysml:Refine",
    "trace": "sysml:Trace"
};

dependencies.forEach(dep => {
    data.content.push({
        "eClass": typeToEClass[dep.type],
        "data": {
            "elementId": `DEP-${dep.from}-${dep.to}`,
            "client": dep.from,
            "supplier": dep.to,
            "createdAt": "2025-08-29T11:00:00Z"
        }
    });
});

console.log(JSON.stringify(data, null, 2));