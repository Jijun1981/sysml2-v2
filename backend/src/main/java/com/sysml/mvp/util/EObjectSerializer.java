package com.sysml.mvp.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.io.IOException;
import java.util.List;

/**
 * 自定义EMF对象序列化器 - 参考SysON实现
 * 避免序列化ResourceSet导致的循环引用
 */
public class EObjectSerializer extends JsonSerializer<EObject> {
    
    @Override
    public void serialize(EObject value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        gen.writeStartObject();
        
        // 写入eClass类型 - 使用简化的本地URI
        String eClassURI = "urn:your:sysml2#//" + value.eClass().getName();
        gen.writeStringField("eClass", eClassURI);
        
        // 序列化所有属性
        for (EStructuralFeature feature : value.eClass().getEAllStructuralFeatures()) {
            if (feature instanceof EAttribute) {
                serializeAttribute(value, gen, (EAttribute) feature);
            }
            // 暂时不序列化引用，避免循环引用
            // 后续可以只序列化ID引用
        }
        
        gen.writeEndObject();
    }
    
    private void serializeAttribute(EObject object, JsonGenerator gen, EAttribute attribute) 
            throws IOException {
        Object value = object.eGet(attribute);
        if (value == null) {
            return;
        }
        
        String attrName = attribute.getName();
        
        if (attribute.isMany()) {
            // 多值属性
            gen.writeArrayFieldStart(attrName);
            List<?> list = (List<?>) value;
            for (Object item : list) {
                writeValue(gen, item);
            }
            gen.writeEndArray();
        } else {
            // 单值属性
            gen.writeFieldName(attrName);
            writeValue(gen, value);
        }
    }
    
    private void writeValue(JsonGenerator gen, Object value) throws IOException {
        if (value instanceof String) {
            gen.writeString((String) value);
        } else if (value instanceof Integer) {
            gen.writeNumber((Integer) value);
        } else if (value instanceof Boolean) {
            gen.writeBoolean((Boolean) value);
        } else if (value instanceof java.util.Date) {
            gen.writeString(value.toString());
        } else if (value != null) {
            gen.writeString(value.toString());
        }
    }
}