package com.github.liuanxin.query.util;

import com.github.liuanxin.query.annotation.ColumnInfo;
import com.github.liuanxin.query.annotation.TableInfo;
import com.github.liuanxin.query.model.FunctionSerialize;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class QueryLambdaUtil {

    private static final Map<String, Class<?>> CLASS_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Field> CLASS_FIELD_MAP = new ConcurrentHashMap<>();

    private static <T> SerializedLambda toLambdaMataInfo(FunctionSerialize<T, ?> func) {
        try {
            Method lambdaMethod = func.getClass().getDeclaredMethod("writeReplace");
            // noinspection deprecation
            boolean accessible = lambdaMethod.isAccessible();
            if (!accessible) {
                lambdaMethod.setAccessible(true);
            }
            SerializedLambda lambda = (SerializedLambda) lambdaMethod.invoke(func);
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
        if (!methodName.startsWith("is") && !methodName.startsWith("get")) {
            throw new RuntimeException("method(" + methodName + ") is not a get-method of a property");
        }

        String className = clazz.getName();
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

    public static <T> Class<?> lambdaToClass(FunctionSerialize<T, ?> func) {
        return lambdaToClass(toLambdaMataInfo(func));
    }

    public static <T> Field lambdaToField(FunctionSerialize<T, ?> func) {
        SerializedLambda lambda = toLambdaMataInfo(func);
        return methodToField(lambdaToClass(lambda), lambda.getImplMethodName());
    }

    public static <T> String lambdaToTable(String tablePrefix, FunctionSerialize<T, ?> func) {
        Class<?> clazz = lambdaToClass(func);
        TableInfo tableInfo = clazz.getAnnotation(TableInfo.class);
        if (QueryUtil.isNull(tableInfo) || tableInfo.ignore()) {
            return QueryUtil.classToTableName(tablePrefix, clazz.getSimpleName());
        } else {
            return tableInfo.value();
        }
    }

    public static <T> String lambdaToColumn(FunctionSerialize<T, ?> func) {
        Field field = lambdaToField(func);
        ColumnInfo columnInfo = field.getAnnotation(ColumnInfo.class);
        if (QueryUtil.isNull(columnInfo) || columnInfo.ignore()) {
            return QueryUtil.fieldToColumnName(field.getName());
        } else {
            return columnInfo.value();
        }
    }
}
