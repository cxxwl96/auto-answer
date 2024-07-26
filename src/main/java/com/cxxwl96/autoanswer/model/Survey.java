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
 * 问卷
 *
 * @author cxxwl96
 * @since 2024/3/3 00:44
 */
@Data
public class Survey {
    private String id;

    private String url;

    private String domain;

    // 问卷标题
    private String topic;

    // 描述
    private String description;

    // 题目
    private List<Subject> subjects;

    // 问卷数量
    private int count;

    // 是否指定代理
    private boolean assignProxy;

    // 省
    private String province;

    // 市
    private String city;
}
