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

package com.cxxwl96.autoanswer.context;

import lombok.Getter;

/**
 * Setting
 *
 * @author cxxwl96
 * @since 2024/3/2 17:54
 */
@Getter
public class Setting {
    @Value("view.logger.buffer")
    private int loggerBuffer;

    @Value("proxy.key")
    private String proxyKey;

    @Value("proxy.province")
    private String proxyProvince;

    @Value("schedule.survey.segment")
    private String scheduleSurveySegment;

    @Value("schedule.survey.submit")
    private String scheduleSurveySubmit;
}
