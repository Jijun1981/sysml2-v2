# 简化文本字段需求文档

## 需求编号
REQ-TEXT-SIMPLE-001

## 需求背景
当前系统使用复杂的Documentation对象来存储需求描述文本，涉及EMF引用关系和派生字段，增加了系统复杂度。决定采用简化方案，直接使用SysML标准字段。

## 需求描述
系统应使用SysML标准的`declaredName`和`declaredShortName`字段来存储需求的描述文本和名称，避免使用复杂的Documentation对象。

## 功能需求

### REQ-TEXT-SIMPLE-001-1: 字段映射
- **declaredShortName**: 存储需求的简短名称/标识符
- **declaredName**: 存储需求的详细描述文本
- 两个字段都是Element基类的标准String字段

### REQ-TEXT-SIMPLE-001-2: 前端显示
- 编辑对话框中将"描述"字段映射到`declaredName`
- 表格视图中显示`declaredName`作为描述列
- 树视图悬停提示中显示`declaredName`内容

### REQ-TEXT-SIMPLE-001-3: 后端处理
- 移除所有Documentation对象创建逻辑
- 移除`setDocumentation()`相关方法
- 直接使用标准的`setAttributeIfExists()`处理`declaredName`

### REQ-TEXT-SIMPLE-001-4: 数据持久化
- `declaredName`字段直接存储到JSON的data对象中
- 无需创建额外的Documentation对象
- 保持数据结构简洁

### REQ-TEXT-SIMPLE-001-5: 编辑功能
- 支持通过编辑对话框修改`declaredName`内容
- 修改后能正确保存到后端
- 重新加载后能正确显示

## 验收标准

1. 创建需求时能设置`declaredName`作为描述
2. 编辑需求时能修改`declaredName`内容
3. 保存后数据正确持久化到JSON文件
4. 重启应用后数据能正确加载显示
5. 不再生成Documentation对象

## 测试场景

### 场景1: 创建需求
- 输入: declaredShortName="REQ-001", declaredName="这是一个测试需求的详细描述"
- 预期: JSON中存储这两个字段，无Documentation对象

### 场景2: 编辑需求
- 前置: 已存在需求
- 操作: 修改declaredName为"修改后的描述文本"
- 预期: JSON中declaredName字段更新

### 场景3: 显示需求
- 前置: 已存在带描述的需求
- 预期: 表格和编辑框中正确显示declaredName内容