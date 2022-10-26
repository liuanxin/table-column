package com.github.liuanxin.query.util;

import com.github.liuanxin.query.annotation.ColumnInfo;
import com.github.liuanxin.query.annotation.TableInfo;
import com.github.liuanxin.query.function.FunctionSerialize;
import com.github.liuanxin.query.function.SupplierSerialize;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class QueryLambdaUtil {

    private static final Map<String, Class<?>> CLASS_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Field> CLASS_FIELD_MAP = new ConcurrentHashMap<>();

    private static SerializedLambda getLambdaMataInfo(Serializable obj) {
        try {
            Method lambdaMethod = obj.getClass().getDeclaredMethod("writeReplace");
            // noinspection deprecation
            boolean accessible = lambdaMethod.isAccessible();
            if (!accessible) {
                lambdaMethod.setAccessible(true);
            }
            SerializedLambda lambda = (SerializedLambda) lambdaMethod.invoke(obj);
            if (!accessible) {
                lambdaMethod.setAccessible(false);
            }
            return lambda;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("get lambda method exception", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("invoke lambda method exception", e);
        }
    }

    private static Class<?> lambdaToClass(SerializedLambda lambda) {
        String className = lambda.getImplClass();
        try {
            Class<?> clazz = CLASS_MAP.get(className);
            if (clazz == null) {
                Class<?> useClazz = Class.forName(className.replace("/", "."));
                CLASS_MAP.put(className, useClazz);
                return useClazz;
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("no Class(" + className + ")", e);
        }
    }

    private static Field methodToField(Class<?> clazz, String methodName) {
        String className = clazz.getName();
        if (!methodName.startsWith("is") && !methodName.startsWith("get")) {
            throw new RuntimeException("method(" + methodName + ") in(" + className + ") is not a get-method of a property");
        }

        String fieldMethodName = methodName.substring(methodName.startsWith("is") ? 2 : 3);
        String fieldName = fieldMethodName.substring(0, 1).toLowerCase() + fieldMethodName.substring(1);
        try {
            String key = className + "-->" + fieldName;
            Field field = CLASS_FIELD_MAP.get(key);
            if (field == null) {
                Field useField = clazz.getDeclaredField(fieldName);
                CLASS_FIELD_MAP.put(key, useField);
                return useField;
            }
            return field;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Class(" + className + ") no field(" + fieldName + ")", e);
        }
    }

    public static <T> Class<?> lambdaToClass(SupplierSerialize<T> supp) {
        return lambdaToClass(getLambdaMataInfo(supp));
    }
    public static <T> Class<?> lambdaToClass(FunctionSerialize<T, ?> func) {
        return lambdaToClass(getLambdaMataInfo(func));
    }

    public static <T> Field lambdaToField(SupplierSerialize<T> supp) {
        SerializedLambda lambda = getLambdaMataInfo(supp);
        return methodToField(lambdaToClass(lambda), lambda.getImplMethodName());
    }
    public static <T> Field lambdaToField(FunctionSerialize<T, ?> func) {
        SerializedLambda lambda = getLambdaMataInfo(func);
        return methodToField(lambdaToClass(lambda), lambda.getImplMethodName());
    }

    public static <T> String lambdaFieldToTableName(SupplierSerialize<T> supp) {
        return lambdaFieldToTableName("", supp);
    }
    public static <T> String lambdaFieldToTableName(String tablePrefix, SupplierSerialize<T> supp) {
        return toTableName(tablePrefix, lambdaToClass(supp));
    }
    private static String toTableName(String tablePrefix, Class<?> clazz) {
        TableInfo tableInfo = clazz.getAnnotation(TableInfo.class);
        if (QueryUtil.isNull(tableInfo)) {
            return QueryUtil.classToTableName(tablePrefix, clazz.getSimpleName());
        } else {
            return tableInfo.ignore() ? "" : tableInfo.value();
        }
    }
    public static <T> String lambdaFieldToTableName(FunctionSerialize<T, ?> func) {
        return lambdaFieldToTableName("", func);
    }
    public static <T> String lambdaFieldToTableName(String tablePrefix, FunctionSerialize<T, ?> func) {
        return toTableName(tablePrefix, lambdaToClass(func));
    }

    public static <T> String lambdaFieldToColumnName(SupplierSerialize<T> supp) {
        return toColumnName(lambdaToField(supp));
    }
    private static String toColumnName(Field field) {
        ColumnInfo columnInfo = field.getAnnotation(ColumnInfo.class);
        if (QueryUtil.isNull(columnInfo)) {
            return QueryUtil.fieldToColumnName(field.getName());
        } else {
            return columnInfo.ignore() ? "" : columnInfo.value();
        }
    }
    public static <T> String lambdaFieldToColumnName(FunctionSerialize<T, ?> func) {
        return toColumnName(lambdaToField(func));
    }
}
