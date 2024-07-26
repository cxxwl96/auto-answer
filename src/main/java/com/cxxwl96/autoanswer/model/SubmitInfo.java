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

import lombok.Data;

/**
 * ProxyInfo
 *
 * @author cxxwl96
 * @since 2024/3/21 22:50
 */
@Data
public class SubmitInfo {
    private ProxyType proxyType;

    private String province;

    private String city;

    private UserAgentType userAgentType;

    private boolean autoSubmit;

    private boolean autoClose;
}
