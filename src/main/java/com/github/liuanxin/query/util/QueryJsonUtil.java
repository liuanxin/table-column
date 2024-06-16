package com.github.liuanxin.query.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.liuanxin.query.model.ReqResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class QueryJsonUtil {

    private static final TypeReference<Map<String, ReqResult>> INNER_RESULT_TYPE = new TypeReference<Map<String, ReqResult>>() {};
    private static final TypeReference<Map<String, List<String>>> DATE_FORMAT_RESULT_TYPE = new TypeReference<Map<String, List<String>>>() {};
    private static final TypeReference<Map<String, Object>> DATA_RESULT_TYPE = new TypeReference<Map<String, Object>>() {};
    private static final TypeReference<List<Map<String, Object>>> DATA_LIST_RESULT_TYPE = new TypeReference<List<Map<String, Object>>>() {};

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception ignore) {
            return null;
        }
    }

    public static <T> T toObject(String json, Class<T> clazz) {
        if (QueryUtil.isEmpty(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public static <S,T> T convert(S source, Class<T> clazz) {
        String json = toJson(source);
        if (QueryUtil.isEmpty(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception ignore) {
            return null;
        }
    }

    public static <S,T> List<T> convertList(S source, Class<T> clazz) {
        if (QueryUtil.isNull(source)) {
            return null;
        }
        String json = toJson(source);
        if (QueryUtil.isEmpty(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception ignore) {
            return null;
        }
    }

    public static <S,T> T convertType(S source, TypeReference<T> type) {
        if (QueryUtil.isNull(source)) {
            return null;
        }
        String json = toJson(source);
        if (QueryUtil.isEmpty(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (IOException ignore) {
            return null;
        }
    }

    public static Map<String, ReqResult> convertInnerResult(Object obj) {
        return convertType(obj, INNER_RESULT_TYPE);
    }

    public static Map<String, List<String>> convertDateResult(Object obj) {
        return convertType(obj, DATE_FORMAT_RESULT_TYPE);
    }

    public static Map<String, Object> convertData(Object obj) {
        return convertType(obj, DATA_RESULT_TYPE);
    }

    public static List<Map<String, Object>> convertDataList(Object obj) {
        return convertType(obj, DATA_LIST_RESULT_TYPE);
    }
}
