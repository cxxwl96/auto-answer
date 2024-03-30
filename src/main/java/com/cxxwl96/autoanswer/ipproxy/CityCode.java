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

package com.cxxwl96.autoanswer.ipproxy;

import com.alibaba.fastjson.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;

/**
 * CityCode
 *
 * @author cxxwl96
 * @since 2023/11/25 20:31
 */
public class CityCode {
    private static final String FILE_CONF_CITY_CODE = "assets/city-code.json";

    private static final Map<String, Map<String, String>> CITY_CODES_MAP = new LinkedHashMap<>();

    private static final List<String> provinces;

    static {
        JSONObject object = JSONObject.parseObject(ResourceUtil.readUtf8Str(FILE_CONF_CITY_CODE));
        provinces = object.keySet().stream().map(String::toString).collect(Collectors.toList());
        for (String province : provinces) {
            if (!(object.get(province) instanceof JSONObject)) {
                throw new IllegalStateException("City Objects must be JSON objects");
            }
            JSONObject citiesObj = (JSONObject) object.get(province);
            List<String> cities = citiesObj.keySet().stream().map(String::toString).collect(Collectors.toList());

            Map<String, String> cityCodeMap = new LinkedHashMap<>();
            for (String city : cities) {
                Object codeObj = citiesObj.get(city);
                if (!(codeObj instanceof String)) {
                    throw new IllegalStateException("City code must be string");
                }
                cityCodeMap.put(city, (String) codeObj);
            }
            CITY_CODES_MAP.put(province, cityCodeMap);
        }
    }

    /**
     * 获取省列表
     *
     * @return 省列表
     */
    public static List<String> getProvinces() {
        return provinces;
    }

    /**
     * 获取城市列表
     *
     * @param province 省
     * @return 城市列表
     */
    public static List<String> getCities(String province) {
        if (!CITY_CODES_MAP.containsKey(province)) {
            throw new IllegalStateException("No province: " + province);
        }
        return CollUtil.newArrayList(CITY_CODES_MAP.get(province).keySet());
    }

    /**
     * 获取城市编码
     *
     * @param province 省
     * @param city 城市
     * @return 城市编码
     */
    public static String getCode(String province, String city) {
        return CITY_CODES_MAP.getOrDefault(province, MapUtil.newHashMap()).getOrDefault(city, StrUtil.EMPTY);
    }
}
