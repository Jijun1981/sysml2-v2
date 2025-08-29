// 临时调试文件 - 用于查看SysML元模型字段
// 这个文件将被集成到PilotEMFService中

public void debugMetamodel() {
    EPackage sysmlPackage = modelRegistry.getSysMLPackage();
    
    // 打印RequirementDefinition的所有属性
    EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
    if (reqDefClass != null) {
        log.info("=== RequirementDefinition 所有属性 ===");
        
        // 自身属性
        log.info("自身属性 ({}个):", reqDefClass.getEAttributes().size());
        for (EAttribute attr : reqDefClass.getEAttributes()) {
            log.info("  - {}: {} ({})", attr.getName(), attr.getEType().getName(), 
                attr.isRequired() ? "必填" : "可选");
        }
        
        // 继承的所有属性
        log.info("所有属性包括继承 ({}个):", reqDefClass.getEAllAttributes().size());
        for (EAttribute attr : reqDefClass.getEAllAttributes()) {
            EClass definingClass = attr.getEContainingClass();
            log.info("  - {}: {} (来自 {})", attr.getName(), attr.getEType().getName(), 
                definingClass.getName());
        }
        
        // 所有结构特征（包括引用）
        log.info("所有结构特征 ({}个):", reqDefClass.getEAllStructuralFeatures().size());
        for (EStructuralFeature feature : reqDefClass.getEAllStructuralFeatures()) {
            String type = feature instanceof EAttribute ? "属性" : "引用";
            EClass definingClass = feature.getEContainingClass();
            log.info("  - {} [{}]: {} (来自 {})", feature.getName(), type, 
                feature.getEType().getName(), definingClass.getName());
        }
        
        // 继承层次
        log.info("继承层次:");
        for (EClass superType : reqDefClass.getEAllSuperTypes()) {
            log.info("  <- {}", superType.getName());
        }
    }
    
    // 打印RequirementUsage的所有属性
    EClass reqUsageClass = (EClass) sysmlPackage.getEClassifier("RequirementUsage");
    if (reqUsageClass != null) {
        log.info("=== RequirementUsage 所有属性 ===");
        
        // 自身属性
        log.info("自身属性 ({}个):", reqUsageClass.getEAttributes().size());
        for (EAttribute attr : reqUsageClass.getEAttributes()) {
            log.info("  - {}: {} ({})", attr.getName(), attr.getEType().getName(),
                attr.isRequired() ? "必填" : "可选");
        }
        
        // 所有结构特征
        log.info("所有结构特征 ({}个):", reqUsageClass.getEAllStructuralFeatures().size());
        for (EStructuralFeature feature : reqUsageClass.getEAllStructuralFeatures()) {
            String type = feature instanceof EAttribute ? "属性" : "引用";
            EClass definingClass = feature.getEContainingClass();
            log.info("  - {} [{}]: {} (来自 {})", feature.getName(), type,
                feature.getEType().getName(), definingClass.getName());
        }
        
        // 继承层次
        log.info("继承层次:");
        for (EClass superType : reqUsageClass.getEAllSuperTypes()) {
            log.info("  <- {}", superType.getName());
        }
    }
}