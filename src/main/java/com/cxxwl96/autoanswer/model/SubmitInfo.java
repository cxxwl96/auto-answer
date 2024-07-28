/*
 * Copyright (c) 2021-2024, cxxwl96.com (cxxwl96@sina.com).
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

package com.cxxwl96.autoanswer.model;

import com.cxxwl96.autoanswer.enums.ProxyType;
import com.cxxwl96.autoanswer.enums.UserAgentType;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * ProxyInfo
 *
 * @author cxxwl96
 * @since 2024/3/21 22:50
 */
@Data
public class SubmitInfo {
    // 域
    private String domain;

    // 问卷链接
    private String url;

    // xlsx文件路径
    private String xlsxPath;

    // 代理类型
    private ProxyType proxyType;

    // 省
    private String province;

    // 市
    private String city;

    // 用户代理类型
    private UserAgentType userAgentType;

    // 是否自动提交问卷
    private boolean autoSubmit;

    // 提交后成功后是否自动关闭浏览器
    private boolean autoClose;

    // n份问卷答案
    private List<Survey> surveys = new ArrayList<>();
}
