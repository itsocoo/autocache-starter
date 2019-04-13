/**
 * @author wanghaibo
 * @version V1.0
 * @desc
 * @date 2018/12/28 17:26
 */
package com.itsocoo.autocache.redis;
// 1.扫描定义的包下面所有符合AOP的方法
// 2.分析出这些方法的参数结构
// 3.将上面的结构生成对用的缓存KEY的spEL
// 4.将生成的KEY缓存在caffeine中
// 5.下次生成缓存先查询存不存在该KEY 存在使用 不存在 生成该KEY后缓存进caffeine中

// TODO: 限制：
// 1.同一个类中的方法名称不能重复 否则自动处理无法计算出具体的方法
// 2.方法中的Map参数类型只能有一个 多个的话 无法具体解析出是什么值组合的缓存key
// 3.暂时不支持参数类型是List
// 4.其他限制 参考com.lzsz.cache.redis.test.service.impl.TestCacheEntityServiceImpl类里面的注释
