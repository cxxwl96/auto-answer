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
 * 题目
 *
 * @author cxxwl96
 * @since 2024/3/3 00:45
 */
@Data
public class Subject {
    // 题目编号
    private int no;

    // 题目类型
    private String type;

    // 题目问题
    private String question;

    // 是否必答题
    private boolean require = true;

    // 多选题选项个数范围
    private int atLeastBegin = 0;

    // 多选题选项个数范围
    private int atLeastEnd = 0;

    // 选项
    private List<Option> options;
}
