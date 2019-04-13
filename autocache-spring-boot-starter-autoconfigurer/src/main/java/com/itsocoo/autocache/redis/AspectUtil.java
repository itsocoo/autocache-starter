package com.itsocoo.autocache.redis;

import com.itsocoo.autocache.redis.common.AspectTools;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author wanghaibo
 * @version V1.0
 * @desc redis的缓存的切面工具类
 * @date 2018/12/26 11:44
 */
public class AspectUtil {
    private static final Logger logger = LoggerFactory.getLogger(AspectUtil.class);

    /**
     * @desc 解析约定的方法的名称
     */
    public static String[] parseMethodName(String methodName) {
        if (StringUtils.isBlank(methodName)) {
            return new String[0];
        }
        // 固定的从CacheBy开始
        String[] strings = methodName.split(AspectTools.PARSE_SPLIT_PREFIX);

        if (strings.length == 2 && StringUtils.isNotBlank(strings[1])) {
            // 首字母小写
            String arg = strings[1];
            // 固定的多个参数and连接
            String[] split = arg.split(AspectTools.PARSE_SPLIT_CONNECT);

            String[] retStr = new String[split.length];

            // 首字母转为小写
            for (int i = 0; i < split.length; i++) {
                retStr[i] = AspectTools.toLowerCaseFirstOne(split[i]);
            }
            return retStr;
        }

        return new String[0];
    }

    /**
     * @desc 将方法的参数解析为spEl
     */
    public static String getPointArgsAction(String methodName, String[] pointParameterNames, Class[] pointParameterTypes, Object[] pointParameterValues) {
        logger.debug("op=start_getPointArgsAction, methodName={}, pointParameterNames={}, pointParameterTypes={}, pointParameterValues={}", methodName, pointParameterNames, pointParameterTypes, pointParameterValues);

        StringBuilder argsBuilder = new StringBuilder();

        String[] methodNames = parseMethodName(methodName);

        // 0.拼接上面的3个参数为一个
        List<Map<String, Object>> paramsMap = AspectTools.inPackageMapClass(pointParameterNames, pointParameterTypes, pointParameterValues);

        List<String> parameterNamesList = Arrays.asList(pointParameterNames);

        // 1.先将参数名称分组 已经存在的一组 不存在的一组
        Map<Boolean, List<String>> partitioningMap = Arrays.stream(methodNames).collect(Collectors.partitioningBy(parameterNamesList::contains));

        logger.debug("===============>partitioningMap:{}", partitioningMap);

        // 2.存在的可能是单个的基本类型 也可以是实体entity 或者Map List  ……
        List<String> trueList = partitioningMap.get(true);
        // 2.1拼接的参数已经在参数列表中则直接以“#xxx”的方式取出来
        // 2.2要判断类型 除基本类型外 都改成"T()"方式
        trueList.forEach(s -> {
            // 获取该参数名称的的对应类型
            Class type = AspectTools.getTypeInMap(paramsMap, s);
            if (type != null) {
                if (AspectTools.isBaseObjectType(type)) {
                    argsBuilder.append("#").append(s);
                } else {
                    // "#T(" + s + ")"
                    argsBuilder.append("#T(").append(s).append(")");
                }
                argsBuilder.append(AspectTools.ARGS_JOINT_MARK);
            }
        });

        // 3.不存在的可能是是实体entity 或者Map List ……
        List<String> falseList = partitioningMap.get(false);

        // 4.这里要过滤掉已经存在的key 然后将里面的Map单独分成一组
        Map<Boolean, List<Map<String, Object>>> booleanListMap = paramsMap.stream().filter(objectMap -> !trueList.contains(objectMap.get("parameterName"))).collect(Collectors.partitioningBy(o -> AspectTools.isBaseMapType(o.get("parameterType"))));

        List<Map<String, Object>> isMapList = booleanListMap.get(true);
        List<Map<String, Object>> notMapList = booleanListMap.get(false);

        // 5.先处理一下不是map的 只能是实体的类了
        List<String> tmpList = new ArrayList<>();
        for (String methodParam : falseList) {
            logger.debug("===============>methodParam:{}", methodParam);

            for (Map<String, Object> objectMap : notMapList) {
                // 参数类型
                Class parameterType = (Class) objectMap.get("parameterType");
                logger.debug("===============>parameterType:{}", parameterType);

                // 参数名称
                String parameterName = (String) objectMap.get("parameterName");
                logger.debug("===============>parameterName:{}", parameterName);

                // 参数值
                Object parameterValue = objectMap.get("parameterValue");
                logger.debug("===============>parameterValue:{}", parameterValue);

                if (AspectTools.isCustomEntityType(parameterType)) {
                    // 如果是实体类型则必须验证实体内部的成员变量名称是否一致
                    List<String> reflectClassFields = reflectClassFields(parameterType);
                    if (reflectClassFields.contains(methodParam)) {
                        // argsBuilder.append("#").append(parameterName).append(".").append(methodParam);
                        argsBuilder.append("#").append(parameterName).append("['").append(methodParam).append("']");
                        argsBuilder.append(AspectTools.ARGS_JOINT_MARK);
                        // 已经被这里处理的临时保存下来
                        tmpList.add(methodParam);
                        break;
                    }
                }
            }
        }

        // 6.在处理剩下的是Map的
        // 去掉上面处理的
        falseList.removeAll(tmpList);

        for (String methodParam : falseList) {
            logger.debug("===============>methodParam:{}", methodParam);

            for (Map<String, Object> objectMap : isMapList) {
                // 参数类型
                Class parameterType = (Class) objectMap.get("parameterType");
                logger.debug("===============>parameterType:{}", parameterType);

                // 参数名称
                String parameterName = (String) objectMap.get("parameterName");
                logger.debug("===============>parameterName:{}", parameterName);

                // 参数值
                Object parameterValue = objectMap.get("parameterValue");
                logger.debug("===============>parameterValue:{}", parameterValue);

                if (AspectTools.isBaseCollectionType(parameterType)) {
                    // 如果是集合
                } else if (AspectTools.isBaseMapType(parameterType)) {
                    // 如果参数pointParameterValues存在 就判断该值是否真实存在在map中
                    if (pointParameterValues != null) {
                        if (parameterValue instanceof Map) {
                            Map<? extends String, ?> map = (Map<? extends String, ?>) parameterValue;
                            if (map.containsKey(methodParam)) {
                                argsBuilder.append("#").append(parameterName).append("['").append(methodParam).append("']");
                                argsBuilder.append(AspectTools.ARGS_JOINT_MARK);
                                break;
                            }
                        }
                    } else {
                        argsBuilder.append("#").append(parameterName).append("['").append(methodParam).append("']");
                        argsBuilder.append(AspectTools.ARGS_JOINT_MARK);
                        break;
                    }
                }

            }
        }

        argsBuilder.deleteCharAt(argsBuilder.lastIndexOf(AspectTools.ARGS_JOINT_MARK));
        return argsBuilder.toString();
    }


    /**
     * @desc 通过实体的类型反射获取类的成员变量
     */
    public static List<String> reflectClassFields(Class type) {
        //获取所有属性
        Field[] fields = type.getDeclaredFields();

        return Arrays.stream(fields).map(field -> {
            String fieldName;
            //获取是否可访问
            boolean flag = field.isAccessible();
            //设置该属性总是可访问
            field.setAccessible(true);
            fieldName = field.getName();
            logger.debug("===============>成员变量:{}", fieldName);
            //还原可访问权限
            field.setAccessible(flag);
            return fieldName;
        }).collect(Collectors.toList());
    }

    /**
     * @desc 解析spEL表达式为具体的值
     * keys spEL表达式
     * method 方法
     * args 方法的参数值
     */
    public static String parseSpELKey2String(String keys, Method method, Object[] args) {
        if (StringUtils.isBlank(keys)) {
            return "";
        }

        int i = keys.indexOf(AspectTools.ARGS_JOINT_MARK + AspectTools.ARGS_JOINT_MARK_AUTO);

        if (i == -1) {
            // xx.*这种情况不处理 直接返回
            return keys;
        }
        // 处理(#id.*)这种情况 去掉.*
        boolean ends = keys.endsWith(AspectTools.ARGS_JOINT_MARK_FUZZY);
        keys = ends ? keys.substring(0, keys.length() - AspectTools.ARGS_JOINT_MARK_FUZZY.length()) : keys;

        String startStr = keys.substring(0, i);
        String endStr = keys.substring(i + 1, keys.length());

        String toString = parseSpELKeyToString(endStr, method, args);
        logger.debug("===============>toString:{}", toString);

        // 将(#id.*)去掉的.*拼接回去
        return AspectTools.joiningKey(startStr, toString) + (ends ? AspectTools.ARGS_JOINT_MARK_FUZZY : "");
    }

    public static String parseSpELKeyToString(String keys, Method method, Object[] args) {
        if (StringUtils.isBlank(keys)) {
            return "";
        }

        String[] strings = keys.split("\\" + AspectTools.ARGS_JOINT_MARK);

        return Arrays.stream(strings).map(s -> parseSpELKey(s, method, args)).reduce((s, s2) -> String.join(AspectTools.ARGS_JOINT_MARK, s, s2)).orElse("");
    }

    // spEL进行参数的解析 获取参数值
    public static String parseSpELKey(String key, Method method, Object[] args) {
        // 获取被拦截方法参数名列表(使用Spring支持类库)
        LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
        String[] paraNameArr = u.getParameterNames(method);

        // 使用spEL进行key的解析
        ExpressionParser parser = new SpelExpressionParser();
        // spEL上下文
        StandardEvaluationContext context = new StandardEvaluationContext();
        // 把方法参数放入spEL上下文中
        for (int i = 0; i < paraNameArr.length; i++) {
            context.setVariable(paraNameArr[i], args[i]);
        }

        // 解析key
        key = parseSpELKey(key);
        // System.out.println("实体T():" + parser.parseExpression("#params.toString()").getValue(context, String.class));

        return parser.parseExpression(key).getValue(context, String.class);
    }

    // 将spEL是'实体'的解析为实体的toString()方法
    public static String parseSpELKey(String key) {
        Matcher matcher = Pattern.compile("\\#T\\((\\w+)\\)").matcher(key);
        return matcher.find() ? "#" + matcher.group(1) + ".toString()" : key;
    }

    /**
     * @desc 将表达式解析为下面3中对应的方式 这样就可以混合处理
     */
    public static Map<String, Set<Map<String, String>>> boostParseMethods(String[] methods) {

        Map<String, Set<Map<String, String>>> actionWay = Arrays.stream(methods).map(method -> {
            if (!method.contains(AspectTools.CACHE_EVICT_VALUES_SEPARATOR)) {
                // ":#"
                method = method + AspectTools.CACHE_EVICT_VALUES_SEPARATOR + AspectTools.ARGS_JOINT_MARK_AUTO;
            }

            String[] strings = method.split("\\" + AspectTools.CACHE_EVICT_VALUES_SEPARATOR);
            Map<String, String> tempMap = new HashMap<>();

            if (strings.length == 2) {
                String mth = strings[0].trim();
                String arg = strings[1].trim();
                tempMap.put("m", mth);

                if (arg.equalsIgnoreCase(AspectTools.ARGS_JOINT_MARK_FUZZY)) {
                    tempMap.put("a", "");
                    tempMap.put("tips", "fuzzy");
                } else if (arg.equalsIgnoreCase(AspectTools.ARGS_JOINT_MARK_AUTO)) {
                    tempMap.put("a", "");
                    tempMap.put("tips", "auto");
                } else if (arg.startsWith(AspectTools.ARGS_JOINT_MARK_AUTO) && arg.endsWith(AspectTools.ARGS_JOINT_MARK_FUZZY)) {
                    tempMap.put("tips", "fuzzyExt");
                    tempMap.put("a", arg);
                } else if (arg.startsWith(AspectTools.ARGS_JOINT_MARK_AUTO)) {
                    tempMap.put("tips", "accurately");
                    tempMap.put("a", arg);
                }

                // switch (arg) {
                //     case AspectTools.ARGS_JOINT_MARK_FUZZY:
                //         tempMap.put("a", "");
                //         tempMap.put("tips", "fuzzy");
                //         break;
                //     case AspectTools.ARGS_JOINT_MARK_AUTO:
                //         tempMap.put("a", "");
                //         tempMap.put("tips", "auto");
                //         break;
                //     default:
                //         tempMap.put("tips", "accurately");
                //         tempMap.put("a", arg);
                //         break;
                // }
            }
            return tempMap;
        }).filter(stringStringMap -> !stringStringMap.isEmpty() && StringUtils.isNotBlank(stringStringMap.get("tips"))).distinct().collect(Collectors.groupingBy(o -> {
            String tips = o.get("tips");
            o.remove("tips");
            return tips;
        }, Collectors.toSet()));

        return actionWay;
    }

    // 解析缓存方法的自定义缓存键 直接将参数解析为值 跳过了先解析为spEL 废弃不用
    @Deprecated
    public static List<Object> parseCustomFields(String methodName, String[] parameterNames, Object[] args) {

        // 先把该方法的参数和值组合在一起
        Map<String, Object> paramsMap = AspectTools.inPackageMap(parameterNames, args);
        logger.debug("===============>paramsMap:{}", paramsMap);

        String[] paramNames = parseMethodName(methodName);

        List<String> parameterNamesList = Arrays.asList(parameterNames);

        // 1.先将参数名称分组 已经存在的一组 不存在的一组
        Map<Boolean, List<String>> partitioningMap = Arrays.stream(paramNames).collect(Collectors.partitioningBy(parameterNamesList::contains));

        logger.debug("===============>partitioningMap:{}", partitioningMap);
        List<String> trueList = partitioningMap.get(true);
        List<String> falseList = partitioningMap.get(false);

        // 1.拼接的参数已经在参数列表中则直接取出来值
        List<Object> argsTrueList = trueList.stream().map(paramsMap::get).collect(Collectors.toList());

        // 2.拼接的参数不在参数列表中的则一定是某个参数的成员变量 则取出改成员变量的值
        List<Object> argsFalseList = falseList.stream().flatMap(name -> {
            String n = AspectTools.toLowerCaseFirstOne(name);
            logger.debug("===============>n:{}", n);
            return paramsMap.values().stream().map(o -> {

                if (o instanceof Map) {
                    // 如果参数是Map则
                    Map<? extends String, ?> map = (Map<? extends String, ?>) o;
                    Object obj = map.get(n);
                    logger.debug("===============>obj:{}", obj);
                    return obj;
                } else {
                    // 如果为实体则解析出里面的参数值返回 使用实体的getXxx();方法获取值
                    Method[] methods = o.getClass().getDeclaredMethods();

                    for (Method mth : methods) {
                        if (mth.getName().equals("get" + name)) {
                            try {
                                Object obj = mth.invoke(o);
                                logger.debug("===============>obj:{}", obj);
                                return obj;
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    logger.debug("===============>methods:{}", Arrays.asList(methods));
                }
                return null;
            }).filter(Objects::nonNull);

        }).collect(Collectors.toList());

        logger.debug("===============>argsTrueList:{}", argsTrueList);
        logger.debug("===============>argsFalseList:{}", argsFalseList);

        argsTrueList.addAll(argsFalseList);

        return argsTrueList;
    }

    // 解析缓存方法的自定义缓存键 spEL  废弃不用
    @Deprecated
    public static String parseCustomFieldsToSpEL(String methodName, String[] pointParameterNames, Class[] pointParameterTypes) {
        logger.debug("op=start_parseCustomFieldsToSpEL, methodName={}, pointParameterNames={}", methodName, pointParameterNames);

        // 先把该方法的参数和参数类型组合在一起
        Map<String, Class> paramsMap = AspectTools.inPackageMapClass(pointParameterNames, pointParameterTypes);
        logger.debug("===============>paramsMap:{}", paramsMap);

        String[] paramNames = parseMethodName(methodName);
        List<String> parameterNamesList = Arrays.asList(pointParameterNames);

        logger.debug("===============>paramNames:{}", Arrays.asList(paramNames));
        // 1.先将参数名称分组 已经存在的一组 不存在的一组
        Map<Boolean, List<String>> partitioningMap = Arrays.stream(paramNames).collect(Collectors.partitioningBy(parameterNamesList::contains));

        logger.debug("===============>partitioningMap:{}", partitioningMap);
        List<String> trueList = partitioningMap.get(true);
        List<String> falseList = partitioningMap.get(false);

        // 1.拼接的参数已经在参数列表中则直接以“#xxx”的方式取出来
        List<String> argsTrueList = trueList.stream().map(s -> "#" + s).collect(Collectors.toList());

        logger.debug("===============>argsTrue  List:{}", argsTrueList);

        logger.debug("===============>pointParameterTypes:{}", pointParameterTypes);

        // 这里要过滤掉已经存在的key
        List<String> stringStream = paramsMap.keySet().stream().filter(s -> !trueList.contains(s)).collect(Collectors.toList());

        // 2.拼接的参数不在参数列表中的则一定是某个参数的成员变量 则判断是否存在该成员变量
        List<String> argsFalseList = falseList.stream().flatMap(name -> {
            logger.debug("===============>name:{}", name);

            // 判断参数的类型
            List<String> stringList = new ArrayList<>();

            // TODO: 多个实体中都存在该key则只取第一个实体中的。忽略后面的
            for (String key : stringStream) {

                Class<?> aClass = paramsMap.get(key);
                String simpleName = aClass.getSimpleName();
                if (AspectTools.isCustomEntityType(aClass)) { // 基本类型
                    // 如果为实体则解析出里面的参数值返回 使用实体的getXxx();方法获取值
                    Field[] declaredFields = aClass.getDeclaredFields();
                    List<String> collect = Arrays.stream(declaredFields).map(Field::getName).collect(Collectors.toList());

                    if (collect.contains(name)) {
                        stringList.add("#" + key + "['" + name + "']");
                        break;
                    }
                } else if (AspectTools.isBaseCollectionType(aClass)) { //java集合类型
                    if (simpleName.equalsIgnoreCase("map")) {
                        // 如果参数是Map则 此处没有判断是否存在
                        stringList.add("#" + key + "['" + name + "']");
                        break;
                    } else if (simpleName.equalsIgnoreCase("list")) {
                        // TODO: 2019/1/10
                    }
                } else if (AspectTools.isBaseObjectType(aClass)) {
                }
            }
            return stringList.stream();
        }).collect(Collectors.toList());

        logger.debug("===============>argsTrueList:{}", argsTrueList);
        logger.debug("===============>argsFalseList:{}", argsFalseList);
        argsTrueList.addAll(argsFalseList);
        logger.debug("===============>argsTrueList:{}", argsTrueList);

        // 拼接最后的spEL字符串
        return argsTrueList.stream().reduce((o, o2) -> String.join(AspectTools.ARGS_JOINT_MARK, o, o2)).orElse("");
    }


    public static void main(String[] args) {
        //
        //     String keys = "com.lzsz.query.gift.service.impl.findDetailCacheByOrderId.#orderId.#id";
        //
        //     int i = keys.indexOf(AspectTools.ARGS_JOINT_MARK + AspectTools.ARGS_JOINT_MARK_AUTO);
        //     String startStr = keys.substring(0, i);
        //     String endStr = keys.substring(i + 1, keys.length());
        //
        //     System.out.println(startStr);
        //     System.out.println(endStr);

        String key = "#(params)";
        String group = parseSpELKey(key);

        System.out.println(group);


        // String s = key.replaceAll("[\\((\\w+)\\)]", "$0.toString()");
        // System.out.println(s);

    }


}
