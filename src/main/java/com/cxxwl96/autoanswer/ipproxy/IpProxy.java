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

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.cxxwl96.autoanswer.context.AutoAnswerContext;
import com.cxxwl96.autoanswer.context.Setting;
import com.cxxwl96.autoanswer.utils.CodePart;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * IpProxy
 *
 * @author cxxwl96
 * @since 2023/11/25 21:54
 */
@Slf4j
public class IpProxy {
    public static ResultBody proxy(String province, String city, int count) {
        Map<String, Object> formMap = form(province, city, count, "json");
        String url = "http://find.xiaoxiongip.com/find_http";
        log.info("[IPProxy] Request: {}?{}", url, HttpUtil.toParams(formMap));

        return CodePart.retryWithRuntimeException(5, 1000, () -> {
            try (HttpResponse response = HttpUtil.createGet(url).form(formMap).execute()) {
                if (!response.isOk()) {
                    log.error("[IPProxy] Failed to execute: {}", response);
                    throw new IllegalStateException("Failed to execute");
                }
                String body = response.body();
                log.info("[IPProxy] Response: {}", body);
                ResultBody result = JSON.parseObject(body, ResultBody.class);
                result.setAddress(province + " " + city);
                result.setCreateTime(LocalDateTime.now());
                result.setLink(url + "?" + HttpUtil.toParams(form(province, city, count, "text")));
                return result;
            }
        });
    }

    private static Map<String, Object> form(String province, String city, int count, String type) {
        Setting setting = AutoAnswerContext.getSetting();
        Map<String, Object> formMap = new HashMap<>();
        formMap.put("key", setting.getProxyKey());
        formMap.put("count", count);
        formMap.put("type", type);
        formMap.put("only", 1);
        formMap.put("province", setting.getProxyProvince());
        formMap.put("city", CityCode.getCode(province, city));
        formMap.put("textSep", 1);
        formMap.put("pw", "no");
        formMap.put("updateWhite", "yes");
        return formMap;
    }
}
