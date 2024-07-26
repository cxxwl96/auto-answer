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

function executor(survey, subjectAnswers) {
    var subjectEl = document.querySelectorAll("fieldset .ui-field-contain");
    var len = subjectEl.length;
    var index = 0;
    for (var i = 0; i < len; i++) {
        // 1、单选题、多选题、打分题
        if ($(subjectEl[i]).find(".ui-radio").length > 0 || $(subjectEl[i]).find(".ui-checkbox").length > 0
            || $(subjectEl[i]).find('.scale-rating').length > 0) {

            var subjectAnswer = subjectAnswers[index++];
            if (!subjectAnswer.answers || subjectAnswer?.answers?.length === 0) {
                continue;
            }
            var  optionEl;
            if ($(subjectEl[i]).find(".ui-radio").length > 0) {
                // 单选题选项
                 optionEl = $(subjectEl[i]).find(".ui-radio");
            } else if ($(subjectEl[i]).find(".ui-checkbox").length > 0){
                // 多选题选项
                 optionEl = $(subjectEl[i]).find(".ui-checkbox");
            } else if ($(subjectEl[i]).find('.scale-rating').length > 0) {
                // 打分题
                optionEl = $(subjectEl[i]).find('.scale-rating ul li a');
            } else {
                continue;
            }
            for (let j = 0; j < subjectAnswer.answers.length; j++) {
                var answer = subjectAnswer.answers[j];
                optionEl[answer.charCodeAt() - 65].click();
            }
        }

        // 2、单选多题：处理为单选题
        else if ($(subjectEl[i]).find("tbody").length > 0) {
            var tbody = $(subjectEl[i]).find("tbody")[0];
            for (let j = 0; j < tbody.children.length; j++) {
                var tr = tbody.children[j];
                if (tr.hasAttribute("rowindex")) {
                    var subjectAnswer = subjectAnswers[index++];
                    var a = $(tr).find("td a")[subjectAnswer.answers[0]?.charCodeAt() - 65];
                    $(a).click();
                }
            }
        }

        // 3、填空题：处理为单选题
        else if ($(subjectEl[i]).find(".ui-input-text").length > 0 || $(subjectEl[i]).find("textarea").length > 0) {
            var subjectAnswer = subjectAnswers[index++];
            var input = $(subjectEl[i]).find(".ui-input-text input")[0] || $(subjectEl[i]).find("textarea")[0];
            // 模拟用户输入
            input.value = survey.subjects[i]?.options[subjectAnswer.answers[0].charCodeAt() - 65]?.content;
            // 触发input事件，通知文本已经改变
            var event = new Event('input', {bubbles: true});
            input.dispatchEvent(event);
        }
    }
    return "OK";
}

// arguments[0]: 问卷
// arguments[1]: 题目答案
// noinspection JSAnnotator 此处返回给JAVA，IDEA报错忽略
return executor(JSON.parse(arguments[0]), JSON.parse(arguments[1]));