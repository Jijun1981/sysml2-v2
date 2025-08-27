# 文档管理指南

## 文档结构

```
docs/
├── requirements/           # 需求文档
│   ├── 需求文档.md        # MVP需求（冻结）
│   └── phase1-requirements.md  # Phase 1新增需求
├── stories/               # 用户故事
│   ├── *.md              # MVP故事（冻结）
│   └── phase1/           # Phase 1故事
├── design/               # 设计文档
│   ├── architecture.md   # 架构设计（持续更新）
│   └── phase1-template-design.md  # Phase 1设计
├── releases/             # 发布文档
│   └── MVP-v1.0-release-notes.md
├── planning/             # 规划文档
│   └── bottom-up-roadmap.md
└── research/             # 研究报告
    └── sysml2-requirements-model-research-report.md
```

## 文档管理原则

### 1. MVP文档（冻结）
- **状态**：只读，作为基线保存
- **位置**：requirements/需求文档.md, stories/*.md
- **用途**：历史记录，回溯参考

### 2. 增量开发文档（活跃）
- **Phase 1**: docs/design/phase1-*.md
- **Phase 2**: docs/design/phase2-*.md
- **Phase 3**: docs/design/phase3-*.md

### 3. Story+Req管理
```yaml
# tracking-matrix.yaml
epics:
  EPIC-A~F: MVP功能（已完成）
  EPIC-G: Phase 1 模板功能（进行中）
  EPIC-H: Phase 2 关系增强（计划中）
  EPIC-I: Phase 3 继承重定义（计划中）
```

### 4. 版本对应关系
| 版本 | 文档范围 | Epic | Story | Requirement |
|-----|---------|------|-------|-------------|
| v1.0 | MVP | A-F | 1-30 | C1-B5 |
| v1.1 | Phase 1 | G | P1-1~5 | P1-1~8 |
| v1.2 | Phase 2 | H | P2-1~4 | P2-1~6 |
| v1.3 | Phase 3 | I | P3-1~5 | P3-1~7 |

## 工作流程

### 开始新功能
1. 创建设计文档：`docs/design/phaseX-feature.md`
2. 更新tracking-matrix.yaml
3. 创建feature分支

### 完成功能
1. 更新release notes
2. 合并到develop分支
3. 打版本tag

### 文档评审
- 设计文档：开发前评审
- 需求文档：Sprint开始前冻结
- Release Notes：发布前更新