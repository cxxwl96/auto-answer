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

function submit(isAutoSubmit) {
    // 点击提交按钮
    if (isAutoSubmit) {
        // 滚动屏幕
        window.scrollTo(0, 0);
        var scroll = 0;
        var interval = setInterval(()=>{
            window.scrollTo(0, window.scrollY+20);
            console.log(scroll);
            if(window.scrollY===scroll) {
                console.log('clear');
                clearInterval(interval);
                setTimeout(()=>{
                    // 提交
                    $('div#divSubmit div#ctlNext')[0].click();
                },5000);
            }
            scroll = window.scrollY;
        }, 10);
    }
    // 检查是否有错误提示
    var errorMsgEl = $('div.errorMessage');
    for (let i = 0; i < errorMsgEl.length; i++) {
        var display = $(errorMsgEl[i]).css('display');
        if (display === 'block') {
            return '还有题目未答题';
        }
    }
    return "OK";
}

// arguments[0]: 是否提交答案
// noinspection JSAnnotator 此处返回给JAVA，IDEA报错忽略
return submit(arguments[0]);