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

package com.cxxwl96.autoanswer.service;

/**
 * SurveyService
 *
 * @author cxxwl96
 * @since 2024/3/3 01:14
 */
public interface SurveyService {
    /**
     * 解析问卷，并保存到survey
     *
     * @param url 问卷地址
     * @param domain 问卷域
     * @return 问卷ID
     */
    String parseSurvey(String url, String domain);

    /**
     * 填写规则
     *
     * @param linkId 链接ID
     * @param count 数量
     */
    void editRule(String linkId, int count);

    /**
     * 提交问卷
     *
     * @param linkId 链接ID
     * @param assignProxy 是否指定代理
     * @param province 代理省
     * @param city 代理城市
     */
    void submit(String linkId, boolean assignProxy, String province, String city);

    /**
     * 保存问卷及答案。提供给js调用
     *
     * @param surveyStr 问卷JSON串
     * @param surveyAnswersStr 问卷答案JSON串
     */
    String saveSurvey(String surveyStr, String surveyAnswersStr);

    /**
     * 生成答案。提供给js调用
     *
     * @param surveyStr 问卷JSON串
     */
    String generateAnswers(String surveyStr);

    /**
     * 检查答案是否填写完成。提供给js调用
     *
     * @param surveyAnswersStr 问卷答案JSON串
     */
    String checkAnswers(String surveyAnswersStr);
}
