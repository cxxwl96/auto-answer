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

function executor(subjectAnswers) {
    var subjectEl = document.querySelectorAll("fieldset .ui-field-contain");
    var len = subjectEl.length;
    var index = 0;
    for (var i = 0; i < len; i++) {
        // 1、单选题、多选题
        if ($(subjectEl[i]).find(".ui-radio").length > 0 || $(subjectEl[i]).find(".ui-checkbox").length > 0) {
            var subjectAnswer = subjectAnswers[index++];
            if (!subjectAnswer.answers || subjectAnswer?.answers?.length === 0) {
                continue;
            }
            var optionEL;
            if ($(subjectEl[i]).find(".ui-radio").length > 0) {
                // 单选题选项
                optionEL = $(subjectEl[i]).find(".ui-radio");
            } else {
                // 多选题选项
                optionEL = $(subjectEl[i]).find(".ui-checkbox");
            }
            for (let j = 0; j < subjectAnswer.answers.length; j++) {
                var answer = subjectAnswer.answers[j];
                optionEL[answer.charCodeAt() - 65].click();
            }
        }

        // 2、单选多题：处理为单选题
        if ($(subjectEl[i]).find("tbody").length > 0) {
            var tbody = $(subjectEl[i]).find("tbody")[0];
            for (let j = 0; j < tbody.children.length; j++) {
                var tr = tbody.children[j];
                if (tr.hasAttribute("rowindex")) {
                    var subjectAnswer = subjectAnswers[index++];
                    var a = $(tr).find("td")[subjectAnswer.answers[0]?.charCodeAt() - 65 + 1];
                    $(a).find("a").click();
                }
            }
        }

        // 3、填空题：处理为单选题
        if ($(subjectEl[i]).find(".ui-input-text").length > 0) {
            var subjectAnswer = subjectAnswers[index++];
            var input = $(subjectEl[i]).find(".ui-input-text input")[0];
            // 模拟用户输入
            input.value = subjectAnswer.answers[0];
            // 触发input事件，通知文本已经改变
            var event = new Event('input', {bubbles: true});
            input.dispatchEvent(event);
        }
    }

    // 点击提交按钮
    $('div#divSubmit div#ctlNext')[0].click();
    // 检查是否有错误提示
    var errorMsgEl = $('div.errorMessage');
    for (let i = 0; i < errorMsgEl.length; i++) {
        var display = $(errorMsgEl[i]).css('display');
        if (display === 'block') {
            return '提交失败，还有题目未答题';
        }
    }
    return "OK";
}

// 题目答案：subjectAnswers
executor(JSON.parse(subjectAnswers))