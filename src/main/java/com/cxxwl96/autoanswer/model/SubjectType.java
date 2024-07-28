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

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * SubjectType
 *
 * @author cxxwl96
 * @since 2024/3/10 21:59
 */
public enum SubjectType {
    SINGLE_CHOICE("单选题"),

    MULTIPLE_CHOICE("多选题"),

    GAP_FILLING("填空题"),
    ;

    @Getter
    @JsonValue
    private final String name;

    SubjectType(String name) {
        this.name = name;
    }

    public static SubjectType getByName(String name) {
        return Arrays.stream(values()).filter(type -> type.equalsName(name)).findAny().orElse(null);
    }

    public boolean equalsName(String name) {
        return StrUtil.equals(getName(), name);
    }
}
