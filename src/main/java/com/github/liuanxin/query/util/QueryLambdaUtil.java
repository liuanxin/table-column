package com.github.liuanxin.query.util;

import com.github.liuanxin.query.annotation.ColumnIgnore;
import com.github.liuanxin.query.annotation.ColumnInfo;
import com.github.liuanxin.query.annotation.TableIgnore;
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

    @SuppressWarnings("deprecation")
    private static SerializedLambda toMataInfo(Serializable obj) {
        try {
            Method lambdaMethod = obj.getClass().getDeclaredMethod("writeReplace");
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
            if (QueryUtil.isNotNull(clazz)) {
                return clazz;
            } else {
                Class<?> useClazz = Class.forName(className.replace("/", "."));
                CLASS_MAP.put(className, useClazz);
                return useClazz;
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("no Class(" + className + ")", e);
        }
    }

    private static Field methodToField(Class<?> clazz, SerializedLambda lambda) {
        String methodName = lambda.getImplMethodName();
        if (!methodName.startsWith("is") && !methodName.startsWith("get") && !methodName.startsWith("set")) {
            throw new RuntimeException("'" + methodName + "' is not an access method for a field");
        }

        String className = clazz.getName();
        String fieldMethodName = methodName.substring(methodName.startsWith("is") ? 2 : 3);
        String fieldName = fieldMethodName.substring(0, 1).toLowerCase() + fieldMethodName.substring(1);
        try {
            String key = className + "-->" + fieldName;
            Field field = CLASS_FIELD_MAP.get(key);
            if (QueryUtil.isNotNull(field)) {
                return field;
            } else {
                Field useField = clazz.getDeclaredField(fieldName);
                CLASS_FIELD_MAP.put(key, useField);
                return useField;
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Class(" + className + ") no field(" + fieldName + ")", e);
        }
    }

    public static <T> Class<?> toClass(SupplierSerialize<T> supplier) {
        return lambdaToClass(toMataInfo(supplier));
    }
    public static <T> Class<?> toClass(FunctionSerialize<T, ?> function) {
        return lambdaToClass(toMataInfo(function));
    }

    public static <T> Field toField(SupplierSerialize<T> supp) {
        SerializedLambda lambda = toMataInfo(supp);
        return methodToField(lambdaToClass(lambda), lambda);
    }
    public static <T> Field toField(FunctionSerialize<T, ?> function) {
        SerializedLambda lambda = toMataInfo(function);
        return methodToField(lambdaToClass(lambda), lambda);
    }

    public static <T> String toTableName(SupplierSerialize<T> supplier) {
        return toTableName("", toClass(supplier));
    }
    public static <T> String toTableName(String tablePrefix, SupplierSerialize<T> supplier) {
        return toTableName(tablePrefix, toClass(supplier));
    }
    private static String toTableName(String tablePrefix, Class<?> clazz) {
        TableIgnore tableIgnore = clazz.getAnnotation(TableIgnore.class);
        if (QueryUtil.isNotNull(tableIgnore) && tableIgnore.value()) {
            return "";
        }

        TableInfo ti = clazz.getAnnotation(TableInfo.class);
        if (QueryUtil.isNotNull(ti)) {
            String tableName = ti.value();
            if (QueryUtil.isNotEmpty(tableName)) {
                return tableName;
            }
        }
        return QueryUtil.classToTableName(tablePrefix, clazz.getSimpleName());
    }
    public static <T> String toTableName(FunctionSerialize<T, ?> function) {
        return toTableName("", toClass(function));
    }
    public static <T> String toTableName(String tablePrefix, FunctionSerialize<T, ?> function) {
        return toTableName(tablePrefix, toClass(function));
    }

    public static <T> String toColumnName(SupplierSerialize<T> supplier) {
        return toColumnName(toField(supplier));
    }
    private static String toColumnName(Field field, ColumnInfo columnInfo) {
        if (QueryUtil.isNotNull(columnInfo)) {
            String columnName = columnInfo.value();
            if (QueryUtil.isNotEmpty(columnName)) {
                return columnName;
            }
        }
        return QueryUtil.fieldToColumnName(field.getName());
    }
    private static String toColumnName(Field field) {
        return toColumnName(field, toColumnInfo(field));
    }
    private static ColumnInfo toColumnInfo(Field field) {
        ColumnIgnore columnIgnore = field.getAnnotation(ColumnIgnore.class);
        if (QueryUtil.isNotNull(columnIgnore) && columnIgnore.value()) {
            return null;
        }
        return field.getAnnotation(ColumnInfo.class);
    }
    public static <T> String toColumnName(FunctionSerialize<T, ?> function) {
        return toColumnName(toField(function));
    }

    public static <T> String toColumnAndAlias(SupplierSerialize<T> supplier) {
        return toColumnAndAlias(toField(supplier));
    }
    public static <T> String toColumnAndAlias(Field field) {
        ColumnInfo columnInfo = toColumnInfo(field);
        String columnName = toColumnName(field, columnInfo);
        if (QueryUtil.isNotNull(columnInfo)) {
            String fieldName = field.getName();
            return fieldName.equals(columnName) ? columnName : (columnName + " AS " + fieldName);
        } else {
            return columnName;
        }
    }
    public static <T> String toColumnAndAlias(FunctionSerialize<T, ?> function) {
        return toColumnAndAlias(toField(function));
    }
}
