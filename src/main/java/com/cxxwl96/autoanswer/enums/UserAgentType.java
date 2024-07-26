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
 * UserAgentType
 *
 * @author cxxwl96
 * @since 2024/3/29 20:59
 */
public enum UserAgentType {
    DEFAULT("默认UA"),
    PC("电脑UA"),
    PHONE("手机UA"),
    ;

    @Getter
    private final String desc;

    UserAgentType(String desc) {
        this.desc = desc;
    }
}
