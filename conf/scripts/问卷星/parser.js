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

/*
 * 解析问卷
 * 题目类型：
 *      单选题
 *      多选题
 *      单选多题（处理为单选题）
 *      填空题
 *      打分题（处理为单选题）
 */
function parser() {
    var survey = {
        topic: document.querySelector("#htitle").innerText,
        description: $(".description")[0] ? $(".description")[0].innerText : '',
        subjects: []
    };
    var subjectEl = document.querySelectorAll("fieldset .ui-field-contain");
    var len = subjectEl.length;
    var index = 0;
    for (var i = 0; i < len; i++) {
        var question = parseQuestion($(subjectEl[i]).find(".topichtml")[0].innerText);
        // 1、单选题、多选题
        if ($(subjectEl[i]).find(".ui-radio").length > 0 || $(subjectEl[i]).find(".ui-checkbox").length > 0) {
            // 题目
            var subject = {
                "no": ++index,
                // 题目类型：单选、多选、填空......
                "type": "",
                // 标题
                "question": question,
                // 选项
                "options": []
            };
            var optionEl;
            if ($(subjectEl[i]).find(".ui-radio").length > 0) {
                subject.type = "单选题";
                optionEl = $(subjectEl[i]).find(".ui-radio");
            } else {
                subject.type = "多选题";
                optionEl = $(subjectEl[i]).find(".ui-checkbox");
            }
            // 处理选项
            for (var j = 0; j < optionEl.length; j++) {
                subject.options.push({
                    "tab": String.fromCharCode(65 + j),
                    "content": parseOptionContent(optionEl[j].children[1].innerText),
                    "defaultSelectance": 0,
                    "rules": []
                });
            }
            // 添加题目
            survey.subjects.push(subject);
        }

        // 2、单选多题：处理为单选题
        else if ($(subjectEl[i]).find("tbody").length > 0) {
            // 题目
            var subjectTemp = {};
            var tbody = $(subjectEl[i]).find("tbody")[0];
            for (let j = 0; j < tbody.children.length; j++) {
                var tr = tbody.children[j];
                // 选项
                if (tr.getAttribute('class') === 'trlabel' && tr.children.length > 0) {
                    var th = tr.children;
                    subjectTemp = {
                        "no": 0,
                        // 题目类型：单选、多选、填空......
                        "type": "单选题",
                        // 标题
                        "question": "",
                        // 选项
                        "options": []
                    };
                    for (let k = 1; k < th.length; k++) {
                        subjectTemp.options.push({
                            "tab": String.fromCharCode(64 + k),
                            "content": parseOptionContent(th[k].innerText),
                            "defaultSelectance": 0,
                            "rules": []
                        });
                    }
                }
                // 标题
                if (tr.hasAttribute("rowindex")) {
                    subjectTemp.no = ++index;
                    subjectTemp.question = question + '。' + parseQuestion($(tr).find(".itemTitleSpan")[0].innerText);
                    survey.subjects.push({...subjectTemp});
                }
            }
        }

        // 3、填空题：处理为单选题
        else if ($(subjectEl[i]).find(".ui-input-text").length > 0 || $(subjectEl[i]).find("textarea").length > 0) {
            // 题目
            survey.subjects.push({
                "no": ++index,
                // 题目类型：单选、多选、填空......
                "type": "填空题",
                // 标题
                "question": parseQuestion($(subjectEl[i]).find(".topichtml")[0].innerText),
                // 选项
                "options": [
                    {
                        "tab": "A",
                        "content": "",
                        "defaultSelectance": 0,
                        "rules": []
                    }
                ]
            });
        }

        // 4、打分题：处理为单选题
        else if ($(subjectEl[i]).find('.scale-rating').length > 0) {
            // 题目
            var subject = {
                "no": ++index,
                // 题目类型：单选、多选、填空......
                "type": "单选题",
                // 标题
                "question": question,
                // 选项
                "options": []
            };
            var optionEl = $(subjectEl[i]).find('.scale-rating ul li a');
            // 处理选项
            for (var j = 0; j < optionEl.length; j++) {
                subject.options.push({
                    "tab": String.fromCharCode(65 + j),
                    "content": $(optionEl[j]).attr('val') + '分 ' + $(optionEl[j]).attr('title'),
                    "defaultSelectance": 0,
                    "rules": []
                });
            }
            // 添加题目
            survey.subjects.push(subject);
        }
    }
    return JSON.stringify(survey); // 此处返回给JAVA
}

/**
 * 处理题目标题
 */
function parseQuestion(question) {
    var no = question.substring(0, question.indexOf('.'));
    if (/^-?\d+?$/.test(no)) {
        return question.substring(question.indexOf('.') + 1);
    }
    return question;
}

/**
 * 处理选项
 */
function parseOptionContent(optStr) {
    // 去掉选项以A\B\C等开头
    var codeAt = optStr.substring(0, 1).charCodeAt();
    if (65 <= codeAt && codeAt <= 90 && optStr.substring(1, 2) === ' ') {
        return optStr.substring(2);
    } else {
        return optStr;
    }
}

parser();

