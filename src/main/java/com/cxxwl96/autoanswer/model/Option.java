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

import java.util.List;

import lombok.Data;

/**
 * 选项
 *
 * @author cxxwl96
 * @since 2024/3/3 00:46
 */
@Data
public class Option {
    // 选项编号
    private String tab;

    // 选项内容
    private String content;

    // 是否排除当前选项
    private boolean disabled;

    // 默认选择率
    private int defaultSelectance;

    // 当前选项选择的规则
    private List<Rule> rules;
}
