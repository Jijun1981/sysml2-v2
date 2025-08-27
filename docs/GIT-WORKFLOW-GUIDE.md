# Git工作流程指南

## 一、分支结构图解

```
远程仓库 (GitHub)                    本地仓库 (你的电脑)
    │                                      │
    ├── main ──────────────────────────── main (生产版本)
    │     └── v1.0.0 (标签)                   └── v1.0.0
    │                                      │
    ├── develop ────────────────────────  develop (开发主线)
    │                                      │
    └── [待推送] ←───────────────────────  fix/mvp-frontend-crud ← 你在这里！
```

## 二、分支管理的基本流程

### 1. 功能开发流程（Feature Flow）

```bash
# 步骤1: 从develop创建功能分支
git checkout develop              # 切换到develop
git pull origin develop           # 拉取最新代码
git checkout -b feature/新功能    # 创建并切换到新分支

# 步骤2: 开发功能
# ... 编写代码 ...
git add .                         # 添加所有更改
git commit -m "feat: 实现XX功能"  # 提交

# 步骤3: 合并回develop
git checkout develop              # 切换回develop
git merge --no-ff feature/新功能  # 合并功能分支
git push origin develop           # 推送到远程

# 步骤4: 删除功能分支（可选）
git branch -d feature/新功能      # 删除本地分支
```

### 2. 修复流程（Hotfix Flow）

```bash
# 紧急修复直接从main创建
git checkout main
git checkout -b hotfix/紧急修复
# ... 修复bug ...
git commit -m "fix: 修复XX问题"

# 合并到main和develop
git checkout main
git merge --no-ff hotfix/紧急修复
git checkout develop
git merge --no-ff hotfix/紧急修复
```

## 三、你当前需要做的操作

### 现在的任务：将MVP前端CRUD合并到develop

```bash
# 1. 先确保当前分支的所有更改都已提交
git status                        # 查看状态
git add .                         # 如有未提交的更改
git commit -m "描述"              # 提交更改

# 2. 切换到develop分支
git checkout develop

# 3. 合并你的修复分支
git merge --no-ff fix/mvp-frontend-crud

# 4. 推送到远程仓库
git push origin develop

# 5. 删除已完成的分支（可选）
git branch -d fix/mvp-frontend-crud
```

## 四、分支命名规范

| 分支类型 | 命名格式 | 用途 | 生命周期 |
|---------|---------|------|----------|
| main | main | 生产环境代码 | 永久 |
| develop | develop | 开发主线 | 永久 |
| feature | feature/功能名 | 新功能开发 | 临时 |
| fix | fix/问题描述 | 修复问题 | 临时 |
| hotfix | hotfix/紧急问题 | 紧急修复 | 临时 |
| release | release/v1.x | 发布准备 | 临时 |

## 五、常用Git命令速查

### 基础操作
```bash
git status              # 查看当前状态
git log --oneline -5    # 查看最近5次提交
git diff                # 查看未暂存的更改
git diff --staged       # 查看已暂存的更改
```

### 分支操作
```bash
git branch              # 查看本地分支
git branch -a           # 查看所有分支（包括远程）
git checkout 分支名     # 切换分支
git checkout -b 新分支  # 创建并切换到新分支
git branch -d 分支名    # 删除分支（已合并）
git branch -D 分支名    # 强制删除分支
```

### 远程操作
```bash
git fetch               # 获取远程更新（不合并）
git pull                # 获取并合并远程更新
git push                # 推送到远程
git push -u origin 分支 # 推送并设置上游分支
```

### 合并操作
```bash
git merge 分支名        # 快进合并
git merge --no-ff 分支  # 创建合并提交
git rebase 分支名       # 变基（高级）
```

## 六、实际工作场景

### 场景1：开始Phase 1开发
```bash
git checkout develop
git pull origin develop
git checkout -b feature/phase1-template
# 开发REQ-P1-1功能...
git add .
git commit -m "feat(template): implement REQ-P1-1 isAbstract marking"
# 继续开发其他功能...
```

### 场景2：每日工作流程
```bash
# 早上开始工作
git checkout feature/phase1-template
git pull origin develop     # 获取develop最新代码
git merge develop           # 合并到当前分支（避免冲突）

# 工作中频繁提交
git add .
git commit -m "feat: 完成XX功能"

# 晚上结束工作
git push origin feature/phase1-template  # 推送到远程备份
```

### 场景3：功能完成后
```bash
# Phase 1全部完成
git checkout develop
git merge --no-ff feature/phase1-template
git push origin develop
git tag -a v1.1.0 -m "Phase 1: Template features"
git push origin v1.1.0
```

## 七、冲突解决

当合并时遇到冲突：

```bash
git merge feature/xxx
# 提示冲突

# 1. 查看冲突文件
git status

# 2. 编辑冲突文件，选择保留的代码
# 查找 <<<<<<< HEAD 标记

# 3. 标记冲突已解决
git add 冲突文件

# 4. 完成合并
git commit -m "merge: 解决冲突"
```

## 八、最佳实践

### ✅ 应该做的
1. **频繁提交**：小步快跑，每完成一个小功能就提交
2. **清晰的提交信息**：使用规范的提交信息格式
3. **定期推送**：每天结束前推送到远程
4. **保持同步**：定期从develop拉取最新代码
5. **代码审查**：重要功能合并前进行review

### ❌ 不要做的
1. **不要直接在main上开发**
2. **不要强制推送**（git push -f）到共享分支
3. **不要提交大文件**（>100MB）
4. **不要提交敏感信息**（密码、密钥等）
5. **不要在未测试的情况下合并到main**

## 九、提交信息规范

```
类型(范围): 简短描述

详细描述（可选）

关联问题（可选）
```

**类型**：
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 代码格式
- refactor: 重构
- test: 测试
- chore: 构建/工具

**示例**：
```bash
git commit -m "feat(template): 实现需求模板标记功能

- 添加isAbstract字段支持
- 实现模板过滤API
- 完成REQ-P1-1需求

Closes #123"
```

## 十、紧急情况处理

### 撤销操作
```bash
git reset HEAD~1        # 撤销最后一次提交（保留更改）
git reset --hard HEAD~1 # 撤销最后一次提交（丢弃更改）
git revert commit-id    # 创建反向提交
```

### 暂存当前工作
```bash
git stash              # 暂存当前更改
git stash pop          # 恢复暂存的更改
git stash list         # 查看暂存列表
```

### 查看历史
```bash
git log --graph --oneline --all  # 图形化查看所有分支
git reflog                        # 查看所有操作历史
```