package com.itsocoo.autocache.redis.common;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author wanghaibo
 * @version V1.0
 * @desc
 * @date 2019/1/8 10:38
 */
public class AspectTools {
    // 约定spEll的参数的连接符
    public final static String ARGS_JOINT_MARK = ".";
    // 约定spEll的参数的前缀模糊查询标识
    public final static String ARGS_JOINT_MARK_FUZZY = "*";
    // 约定spEll的参数的自动查询标识
    public final static String ARGS_JOINT_MARK_AUTO = "#";
    // 解析方法的名称
    public static final String PARSE_SPLIT_PREFIX = "By";
    // 解析方法的名称的连接符
    public static final String PARSE_SPLIT_CONNECT = "And";
    // 删除的方法标识
    // public static final String CACHE_EVICT_TIPS = "update,delete,clean";
    // 缓存的方法标识
    // public static final String CACHE_ENABLED_TIPS = "find,list,load";
    // 删除的方法分隔符
    public static final String CACHE_EVICT_VALUES_SEPARATOR = ":";
    // public static final String CACHE_EVICT_SPEL_SEPARATOR = "|";

    // 定义缓存的切入点
    public static final String CACHE_QUERY_POINT_CUT = "execution(public * com.lzsz.cache.redis.test.service..*.load*(..))||execution(public * com.lzsz.cache.redis.test.service..*.find*(..))||execution(public * com.lzsz.cache.redis.test.service..*.list*(..))";

    // 定义删除缓存的切入点
    public static final String CACHE_EVICT_POINT_CUT = "execution(public * com.lzsz.cache.redis.test.service..*.update*(..))||execution(public * com.lzsz.cache.redis.test.service..*.delete*(..))||execution(public * com.lzsz.cache.redis.test.service..*.clean*(..))";

    // 判断是否是里面的值
    public static boolean ifContainStr(String word, String containsStr) {
        String[] strings = containsStr.split(",");
        for (String string : strings) {
            if (word.toLowerCase().contains(string.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    // 将成对的3个数组合并为一个Map
    public static List<Map<String, Object>> inPackageMapClass(String[] parameterNames, Class[] parameterTypes, Object[] parameterValues) {

        if (parameterValues == null) {
            parameterValues = new Object[parameterNames.length];
        }

        Object[] finalParameterValues = parameterValues;

        return IntStream.range(0, parameterNames.length).boxed().map(i -> {
            String parameterName = parameterNames[i];
            Class parameterType = parameterTypes[i];
            Object parameterValue = finalParameterValues[i];

            Map<String, Object> stringMap = new HashMap<>();
            stringMap.put("parameterName", parameterName);
            stringMap.put("parameterType", parameterType);
            stringMap.put("parameterValue", parameterValue);

            return stringMap;
        }).collect(Collectors.toList());

    }

    // 判断值parameterName是否在上面的Map中 存在则取出parameterType值
    public static Class getTypeInMap(List<Map<String, Object>> paramsMap, String s) {
        Class type = null;
        for (Map<String, Object> map : paramsMap) {
            if (map.get("parameterName").equals(s)) {
                type = (Class) map.get("parameterType");
                break;
            }
        }
        return type;
    }

    // 将成对的2个数组合并为一个Map
    public static Map<String, Object> inPackageMap(String[] parameterNames, Object[] args) {
        return IntStream.range(0, parameterNames.length).boxed().collect(Collectors.toMap(o -> parameterNames[o], o -> args[o]));
    }

    // 将成对的2个数组合并为一个Map
    public static Map<String, Class> inPackageMapClass(String[] parameterNames, Class[] args) {
        return IntStream.range(0, parameterNames.length).boxed().collect(Collectors.toMap(o -> parameterNames[o], o -> args[o]));
    }

    //首字母转小写
    public static String toLowerCaseFirstOne(String s) {
        if (Character.isLowerCase(s.charAt(0)))
            return s;
        else
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
    }

    //首字母转大写
    public static String toUpperCaseFirstOne(String s) {
        if (Character.isUpperCase(s.charAt(0)))
            return s;
        else
            return (new StringBuilder()).append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).toString();
    }

    // 判断数组是否为空
    public static boolean isArrayBoolean(String[] methods) {
        return methods.length > 0 && StringUtils.isNotBlank(methods[0]);
    }

    // 判断是否是自定义的实体类型
    public static boolean isCustomEntityType(Object classObj) {
        return !isBaseCollectionType(classObj) && !isBaseMapType(classObj) && !isBaseObjectType(classObj);
        // return classObj.equals(Class.class);
    }

    // 判断是否是java的集合类型
    public static boolean isBaseCollectionType(Object collectionObj) {
        return collectionObj.equals(List.class) ||
                collectionObj.equals(ArrayList.class) ||
                collectionObj.equals(Set.class) ||
                collectionObj.equals(HashSet.class);
    }

    // 判断是否是java的Map类型
    public static boolean isBaseMapType(Object mapObj) {
        return mapObj.equals(Map.class) ||
                mapObj.equals(HashMap.class);
    }

    // 判断是否是java的基本类型
    public static boolean isBaseObjectType(Object typeObj) {
        return typeObj.equals(Integer.class) ||
                typeObj.equals(String.class) ||
                typeObj.equals(Byte.class) ||
                typeObj.equals(Long.class) ||
                typeObj.equals(Double.class) ||
                typeObj.equals(Float.class) ||
                typeObj.equals(Character.class) ||
                typeObj.equals(Short.class) ||
                typeObj.equals(Boolean.class);
    }

    // 将字符串用‘ARGS_JOINT_MARK’连接
    public static String joiningKey(String... words) {
        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            builder.append(word).append(ARGS_JOINT_MARK);
        }

        builder.deleteCharAt(builder.lastIndexOf(AspectTools.ARGS_JOINT_MARK));
        return builder.toString();
    }

    // 连接字符串 字符首字母大写
    public static String joiningConnect(String... words) {
        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            builder.append(toUpperCaseFirstOne(word));
        }

        return builder.toString();
    }


    // 拼接模糊前缀 key
    public static String keyGeneratorFuzzy(String packageName, String parameterName, String methodName) {
        if (StringUtils.isAnyBlank(methodName, packageName)) {
            return "";
        }
        // 拼接键值
        return joiningKey(packageName, parameterName, methodName + "*");
    }

    // 处理没有缓存标识“By”的方法名
    public static String getNoSplitPrefixString(String[] parameterNames) {
        String[] strings = Arrays.stream(parameterNames).map(s -> "#T(" + s + ")").toArray(String[]::new);
        return AspectTools.joiningKey(strings);
    }

    // 判断方法是否是静态的方法
    public static boolean isStaticMethod(Method method) {
        int modifiers = method.getModifiers();
        return Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers);
    }


    // 拼接 key
    // public static String keyGenerator(String methodName, String packageName, String args) {
    //     if (StringUtils.isBlank(args)) {
    //         return "";
    //     }
    //     // 拼接键值
    //     StringBuilder builder = new StringBuilder().append(packageName).append(AspectTools.ARGS_JOINT_MARK)
    //             .append(methodName).append(AspectTools.ARGS_JOINT_MARK);
    //     builder.append(args).append(AspectTools.ARGS_JOINT_MARK);
    //
    //     builder.deleteCharAt(builder.lastIndexOf(AspectTools.ARGS_JOINT_MARK));
    //     return builder.toString();
    // }

    // public static String keyGenerator(String methodName, String packageName, Object[] args) {
    //     // 拼接键值
    //     StringBuilder builder = new StringBuilder().append(packageName).append(AspectTools.ARGS_JOINT_MARK)
    //             .append(methodName).append(AspectTools.ARGS_JOINT_MARK);
    //     for (Object arg : args) {
    //         builder.append(arg).append(AspectTools.ARGS_JOINT_MARK);
    //     }
    //
    //     builder.deleteCharAt(builder.lastIndexOf(AspectTools.ARGS_JOINT_MARK));
    //     return builder.toString();
    // }

    // public static String keyGeneratorString(String methodName, String packageName, String keys) {
    //     // 拼接键值
    //     return (new StringBuilder()).append(packageName).append(AspectTools.ARGS_JOINT_MARK)
    //             .append(methodName).append(AspectTools.ARGS_JOINT_MARK).append(keys).toString();
    // }


}


