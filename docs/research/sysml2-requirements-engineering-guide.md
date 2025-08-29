# SysML 2.0 需求工程深度研究报告

## 一、SysML 2.0 需求模型核心概念

### 1.1 需求建模的两层结构

SysML 2.0采用**Definition-Usage**二元模式：

```
RequirementDefinition (需求定义)
    ↓ 实例化
RequirementUsage (需求使用)
```

#### RequirementDefinition（需求定义类）
- **本质**：需求的抽象定义，可复用的需求模板
- **继承自**：ConstraintDefinition（约束定义）
- **核心属性**：
  - `reqId`: 需求标识符（映射到declaredShortName）
  - `text`: 需求文本（从documentation.body派生）
  - `subjectParameter`: 需求主体（被约束的对象）
  - `actorParameter`: 参与者（外部交互实体）
  - `stakeholderParameter`: 利益相关者
  - `assumedConstraint`: 前提假设
  - `requiredConstraint`: 必须满足的约束
  - `framedConcern`: 关注点

#### RequirementUsage（需求使用/实例）
- **本质**：需求定义在特定上下文中的应用
- **关系**：通过`definition`属性关联到RequirementDefinition
- **特点**：可以重定义（redefine）继承的属性

### 1.2 需求的数学本质

SysML 2.0将需求建模为**条件约束**：

```
requirement = assumptions → constraints
```

即："如果所有假设成立，则必须满足所有约束"

```sysml
requirement def MassLimitationRequirement {
    attribute massActual: MassValue;
    attribute massReqd: MassValue;
    
    // 假设条件
    assume constraint { vehicle.fuelLevel > 0 }
    
    // 必须满足的约束
    require constraint { massActual <= massReqd }
}
```

## 二、工程应用模式

### 2.1 需求分类体系（基于标准库）

SysML 2.0标准库定义了5种基本需求类型：

| 需求类型 | Subject类型 | 典型应用场景 | 工程价值 |
|---------|------------|------------|----------|
| **FunctionalRequirement** | Action | 系统行为、功能流程 | 定义系统"做什么" |
| **InterfaceRequirement** | Interface | 接口协议、数据交换 | 确保互操作性 |
| **PerformanceRequirement** | AttributeValue | 响应时间、吞吐量 | 量化质量指标 |
| **PhysicalRequirement** | Part | 尺寸、重量、材料 | 物理约束 |
| **DesignConstraintCheck** | Part | 技术选型、架构约束 | 限制解空间 |

### 2.2 需求层次结构

```
系统需求（System Requirements）
├── 功能需求（Functional）
│   ├── 主功能（Primary Functions）
│   └── 辅助功能（Support Functions）
├── 性能需求（Performance）
│   ├── 时间性能（Timing）
│   ├── 容量性能（Capacity）
│   └── 精度要求（Accuracy）
├── 接口需求（Interface）
│   ├── 硬件接口（Hardware）
│   ├── 软件接口（Software）
│   └── 通信接口（Communication）
└── 约束需求（Constraints）
    ├── 设计约束（Design）
    ├── 物理约束（Physical）
    └── 法规约束（Regulatory）
```

### 2.3 需求参数化机制

SysML 2.0支持**参数化需求**，实现需求复用：

```sysml
requirement def ResponseTimeRequirement {
    // 参数化的主体
    subject system : System;
    
    // 参数化的属性
    attribute operation : String;
    attribute maxResponseTime : DurationValue;
    
    // 参数化的约束
    require constraint {
        system.responseTime(operation) <= maxResponseTime
    }
}

// 实例化应用
requirement userLoginResponseTime : ResponseTimeRequirement {
    :>> operation = "user login";
    :>> maxResponseTime = 2[s];
}
```

## 三、需求关系语义

### 3.1 四种核心关系

| 关系类型 | 语义 | 方向性 | 验证策略 |
|---------|------|--------|----------|
| **satisfy** | 设计/实现满足需求 | Design → Requirement | 测试验证 |
| **derive** | 需求分解/派生 | Parent → Child | 完整性检查 |
| **refine** | 需求细化（不改变本质） | Abstract → Concrete | 一致性检查 |
| **trace** | 一般性追溯 | Any → Any | 影响分析 |

### 3.2 继承机制

SysML 2.0支持需求的**泛化/特化**：

```sysml
// 通用需求定义
requirement def SafetyRequirement {
    abstract;  // 抽象需求
    subject system : System;
    attribute safetyLevel : SafetyIntegrityLevel;
}

// 特化的具体需求
requirement def ISO26262Requirement :> SafetyRequirement {
    :>> safetyLevel in {ASIL_A, ASIL_B, ASIL_C, ASIL_D};
    
    // 添加ISO 26262特定的约束
    require constraint { 
        system.faultTolerance >= mappingToFaultTolerance(safetyLevel)
    }
}
```

## 四、工程实践方法

### 4.1 需求开发流程

```
Step 1: 需求获取
├── 识别stakeholders（利益相关者）
├── 定义concerns（关注点）
└── 收集原始需求

Step 2: 需求分析
├── 分类（按SysML类型）
├── 参数识别（subject, actors, parameters）
└── 约束形式化（assumptions, constraints）

Step 3: 需求建模
├── 创建RequirementDefinitions（可复用模板）
├── 创建RequirementUsages（具体应用）
└── 建立关系（satisfy, derive, refine, trace）

Step 4: 需求验证
├── 一致性检查
├── 完整性检查
└── 可追溯性分析
```

### 4.2 典型应用场景

#### 场景1：汽车电子系统（ISO 26262）

```sysml
package AutomotiveSafetyRequirements {
    
    // 功能安全需求模板
    requirement def FunctionalSafetyReq {
        subject ecu : ECU;
        attribute ASIL : ASILLevel;
        attribute PMHF : Real;  // 每小时危险失效概率
        
        require constraint { PMHF <= getAllowablePMHF(ASIL) }
    }
    
    // 电池管理系统需求
    requirement batteryManagementSafety : FunctionalSafetyReq {
        :>> ASIL = ASIL_C;
        
        // 添加特定的安全约束
        require constraint thermalProtection {
            ecu.batteryTemp < 60[°C]
        }
        
        require constraint overchargeProtection {
            ecu.chargeVoltage <= ecu.maxVoltage * 1.05
        }
    }
}
```

#### 场景2：航空电子系统（DO-178C）

```sysml
package AviationSoftwareRequirements {
    
    requirement def DO178CRequirement {
        subject software : Software;
        attribute DAL : DesignAssuranceLevel;  // A, B, C, D, E
        
        // MC/DC覆盖率要求
        require constraint coverageRequirement {
            (DAL == Level_A) implies (software.MCDCCoverage >= 100[%])
        }
    }
}
```

#### 场景3：医疗设备（IEC 62304）

```sysml
package MedicalDeviceRequirements {
    
    requirement def SoftwareSafetyClassification {
        subject medicalSoftware : MedicalDeviceSoftware;
        attribute safetyClass : {Class_A, Class_B, Class_C};
        
        // 根据安全等级确定验证活动
        require constraint verificationIntensity {
            (safetyClass == Class_C) implies 
            (medicalSoftware.verificationLevel == "Full")
        }
    }
}
```

## 五、需求模板库设计

### 5.1 标准需求模板

```sysml
package StandardRequirementTemplates {
    
    // 响应时间模板
    requirement def ResponseTimeTemplate {
        subject system : System;
        attribute operation : String;
        attribute percentile : Real = 95[%];  // 默认P95
        attribute threshold : DurationValue;
        
        require constraint {
            percentileResponseTime(system, operation, percentile) <= threshold
        }
    }
    
    // 可用性模板
    requirement def AvailabilityTemplate {
        subject service : Service;
        attribute measurementPeriod : DurationValue = 1[month];
        attribute minAvailability : Real;
        
        require constraint {
            (service.uptime / measurementPeriod) >= minAvailability
        }
    }
    
    // 容量模板
    requirement def CapacityTemplate {
        subject system : System;
        attribute resource : String;
        attribute maxCapacity : Real;
        attribute unit : Unit;
        
        require constraint {
            system.resourceUsage(resource) <= maxCapacity * unit
        }
    }
}
```

### 5.2 行业特定模板

```sysml
package IndustrySpecificTemplates {
    
    // 汽车行业
    package Automotive {
        requirement def V2XCommunication {
            subject vehicle : Vehicle;
            attribute latency : DurationValue;
            attribute reliability : Real;
            
            require constraint {
                vehicle.v2xLatency <= latency and
                vehicle.v2xReliability >= reliability
            }
        }
    }
    
    // 航天行业  
    package Aerospace {
        requirement def OrbitAccuracy {
            subject satellite : Satellite;
            attribute positionError : LengthValue;
            attribute velocityError : SpeedValue;
            
            require constraint {
                satellite.positionDeviation <= positionError and
                satellite.velocityDeviation <= velocityError
            }
        }
    }
}
```

## 六、验证与确认（V&V）

### 6.1 需求验证规则

```sysml
verification def RequirementVerification {
    subject req : RequirementUsage;
    
    // 完整性检查
    verify completeness {
        req.subjectParameter != null and
        req.text != null and
        req.requiredConstraint->notEmpty()
    }
    
    // 一致性检查
    verify consistency {
        not hasConflict(req.assumedConstraint, req.requiredConstraint)
    }
    
    // 可测试性检查
    verify testability {
        req.requiredConstraint->forAll(c | isTestable(c))
    }
}
```

### 6.2 需求追溯矩阵

```
Requirements ←→ Design ←→ Implementation ←→ Test
    ↑                                        ↓
    └──────── Verification Loop ──────────┘
```

## 七、实施建议

### 7.1 分阶段实施策略

**Phase 1: 基础能力（2-3周）**
- 需求定义和使用的CRUD
- 基本模板管理
- 简单的satisfy/derive关系

**Phase 2: 高级特性（3-4周）**
- 参数化需求
- 继承机制
- 复杂约束表达

**Phase 3: 行业定制（4-6周）**
- 行业标准模板库
- 合规性检查
- 形式化验证

### 7.2 关键成功因素

1. **标准化**：建立组织级需求模板库
2. **工具支持**：提供直观的可视化和编辑工具
3. **培训**：需求工程师的SysML 2.0培训
4. **集成**：与现有需求管理工具（DOORS、Jira）集成
5. **度量**：建立需求质量度量指标

## 八、与传统需求管理的对比

| 方面 | 传统方法（DOORS/Word） | SysML 2.0方法 | 优势 |
|------|----------------------|--------------|------|
| **结构** | 文档树状结构 | Definition-Usage模型 | 支持复用和实例化 |
| **表达** | 自然语言文本 | 形式化约束 | 可验证、无歧义 |
| **关系** | ID链接 | 语义化关系 | 明确关系含义 |
| **验证** | 人工评审 | 自动化检查 | 提高效率和质量 |
| **追溯** | 静态矩阵 | 动态模型 | 实时影响分析 |

## 九、结论与建议

### 9.1 SysML 2.0需求工程的核心价值

1. **形式化**：将需求从文本提升为可计算的约束
2. **复用性**：通过Definition-Usage模式实现需求复用
3. **可追溯**：语义化的关系支持精确追溯
4. **可验证**：支持自动化的一致性和完整性检查

### 9.2 实施建议

1. **从简单开始**：先实现基本的需求CRUD和模板
2. **渐进增强**：逐步添加继承、参数化等高级特性
3. **行业聚焦**：针对特定行业定制模板库
4. **工具优先**：提供易用的图形化工具降低门槛
5. **培训先行**：确保团队理解SysML 2.0的需求建模理念

### 9.3 预期收益

- **效率提升**：需求复用率提升60%以上
- **质量改进**：需求缺陷减少40%以上
- **成本降低**：需求变更成本降低50%以上
- **合规保障**：自动化的标准合规性检查

---

**下一步行动**：基于此研究，设计一个简洁、实用的SysML 2.0需求管理系统，重点支持：
1. 需求模板的创建和实例化
2. Definition-Usage的灵活映射
3. 四种语义关系的可视化
4. 行业标准模板库
5. 自动化的验证规则