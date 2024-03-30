/*
 * Copyright (c) 2021-2023, cxxwl96.com (cxxwl96@sina.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cxxwl96.autoanswer.utils;

import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.util.TypeUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.hutool.core.lang.Assert;

/**
 * ApplicationStore
 *
 * @author cxxwl96
 * @since 2023/11/26 13:57
 */
public class ApplicationStore {
    private static final Map<String, Object> STORE = new ConcurrentHashMap<>();

    public static void register(Class<?> clazz, Object value) {
        register(clazz.getTypeName(), value);
    }

    public static void register(String key, Object value) {
        Assert.notBlank(key, "fail to register, key is blank");
        STORE.put(key, value);
    }

    public static <T> T get(Class<T> clazz) {
        return get(clazz.getTypeName(), clazz);
    }

    public static <T> T get(String key, Class<T> clazz) {
        Object obj = STORE.get(key);
        return TypeUtils.cast(obj, clazz, new ParserConfig());
    }
}
