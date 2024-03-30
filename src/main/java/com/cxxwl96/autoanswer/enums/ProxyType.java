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

package com.cxxwl96.autoanswer.enums;

import lombok.Getter;

/**
 * ProxyType
 *
 * @author cxxwl96
 * @since 2024/3/21 22:44
 */
public enum ProxyType {
    NO_PROXY("不使用代理"),
    RANGE_PROXY("随机代理"),
    ASSIGN_PROXY("指定代理");

    @Getter
    private final String desc;

    ProxyType(String desc) {
        this.desc = desc;
    }
}
