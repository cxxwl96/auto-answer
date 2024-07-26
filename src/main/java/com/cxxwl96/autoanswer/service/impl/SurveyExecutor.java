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

package com.cxxwl96.autoanswer.service.impl;

import com.alibaba.fastjson.JSON;
import com.cxxwl96.autoanswer.context.AutoAnswerContext;
import com.cxxwl96.autoanswer.context.Script;
import com.cxxwl96.autoanswer.context.UserSetting;
import com.cxxwl96.autoanswer.enums.AnswerState;
import com.cxxwl96.autoanswer.ipproxy.CityCode;
import com.cxxwl96.autoanswer.ipproxy.IpProxy;
import com.cxxwl96.autoanswer.ipproxy.ResultBody;
import com.cxxwl96.autoanswer.model.Result;
import com.cxxwl96.autoanswer.model.SubjectAnswer;
import com.cxxwl96.autoanswer.model.SubmitInfo;
import com.cxxwl96.autoanswer.model.Survey;
import com.cxxwl96.autoanswer.utils.CodePart;
import com.cxxwl96.autoanswer.utils.SeleniumUtil;
import com.cxxwl96.autoanswer.utils.TextAreaLog;

import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * SurveyExecutor
 *
 * @author cxxwl96
 * @since 2024/3/19 23:38
 */
@Slf4j
public class SurveyExecutor implements Callable<Result<?>> {
    private static final String EXECUTOR_LOADING_JS = "assets/js/executor-loading.js";

    private static final String EXECUTOR_WINDOW_JS = "assets/js/executor-window.js";

    // 多少毫秒后执行脚本
    private static final int EXECUTE_SCRIPT_AFTER_MS = 500;

    // 每隔多少毫秒执行一次脚本
    private static final int EXECUTE_SCRIPT_INTERVAL_MS = 200;

    // 提交成功后多少毫秒关闭浏览器
    private static final int EXECUTE_SUBMIT_AFTER_MS = 2000;

    private final TextAreaLog logger = TextAreaLog.getLogger(log);

    // 问卷
    private final Survey survey;

    // 问卷答案
    private final List<SubjectAnswer> subjectAnswers;

    // 当前问卷答案索引
    @Getter
    private final int index;

    // 脚本
    private final Script script;

    // 提交信息
    private final SubmitInfo submitInfo;

    private Consumer<AnswerState> doneConsumer;

    public SurveyExecutor(Survey survey, List<SubjectAnswer> subjectAnswers, int index, Script script, SubmitInfo submitInfo) {
        this.survey = survey;
        this.subjectAnswers = subjectAnswers;
        this.index = index;
        this.script = script;
        this.submitInfo = submitInfo;
    }

    @Override
    public Result<?> call() throws Exception {
        AnswerState state = AnswerState.STOP;
        WebDriver driver = null;
        try {
            logger.info("[提交问卷]打开第" + index + "份问卷");
            // 创建WebDriver
            driver = SeleniumUtil.newChromeDriver(getChromeOptions());
            UserSetting userSetting = AutoAnswerContext.getUserSetting();

            // 打开loading
            openLoading(driver, userSetting);

            // 打开问卷
            openAnswer(driver, userSetting);

            // 提交问卷
            String result = submitAnswer(driver);

            if ("OK".equals(result)) {
                state = AnswerState.SUCCESS;
                logger.success("[提交问卷]提交第" + index + "份问卷成功");
                if (submitInfo.isAutoClose()) {
                    CodePart.sleep(EXECUTE_SUBMIT_AFTER_MS);
                    SeleniumUtil.quit(driver); // 关闭浏览器
                }
                return Result.success();
            } else {
                state = AnswerState.FAILED;
                logger.error("[提交问卷]提交第" + index + "份问卷失败：" + result);
                return Result.failed(String.valueOf(result));
            }
        } catch (InterruptedException exception) {
            SeleniumUtil.quit(driver);
            state = AnswerState.STOP;
            return Result.failed("停止第" + index + "份问卷");
        } catch (Error error) {
            state = AnswerState.FAILED;
            log.error(error.getMessage(), error);
            return Result.failed(error.getMessage());
        } catch (Throwable exception) {
            state = AnswerState.FAILED;
            if (exception instanceof TimeoutException) {
                log.warn("打开页面超时");
                logger.error("[提交问卷]提交第" + index + "份问卷失败：打开页面超时");
                return Result.failed("打开页面超时");
            }
            logger.error("[提交问卷]提交第" + index + "份问卷失败：" + new Scanner(exception.getMessage()).nextLine());
            log.error("{}:{}", exception.getClass().getName(), exception.getMessage(), exception);
            return Result.failed(exception.getMessage());
        } finally {
            if (doneConsumer != null) {
                doneConsumer.accept(state);
            }
        }
    }

    private ChromeOptions getChromeOptions() {
        // 创建ChromeOptions
        ChromeOptions chromeOptions = SeleniumUtil.newChromeOptions();
        // 设置代理
        String proxyServer = nextProxyServer();
        if (StrUtil.isNotBlank(proxyServer)) {
            if (proxyServer.matches("^(\\d{1,3}\\.){3}\\d{1,3}:[1-9]\\d{0,4}$")) {
                chromeOptions.addArguments("--proxy-server=" + proxyServer); // 设置代理
            } else {
                log.warn("错误的代理server: {}", proxyServer);
            }
        }
        // 设置UserAgent
        String userAgent = nextUserAgent();
        if (StrUtil.isNotBlank(userAgent)) {
            chromeOptions.addArguments("user-agent=" + userAgent); // 设置UA
        }
        return chromeOptions;
    }

    private String nextProxyServer() {
        String province;
        String city;
        switch (submitInfo.getProxyType()) {
            case RANGE_PROXY: // 随机代理
                // 随机省
                List<String> provinces = CityCode.getProvinces();
                int i = new Random().nextInt(provinces.size());
                province = provinces.get(i);
                // 随机城市
                List<String> cities = CityCode.getCities(province);
                int j = new Random().nextInt(cities.size());
                city = cities.get(j);
                break;
            case ASSIGN_PROXY: // 指定代理
                province = submitInfo.getProvince();
                city = submitInfo.getCity();
                break;
            case NO_PROXY: // 不使用代理
            default:
                return null;
        }
        ResultBody result = IpProxy.proxy(province, city, 1);

        if (result.getStatus() != 0) {
            log.error("代理失败：" + Optional.ofNullable(result.getInfo()).orElse("请求失败"));
            return null;
        }
        List<ResultBody.Server> servers = result.getList();
        if (servers != null && servers.size() > 0) {
            ResultBody.Server server = servers.get(0);
            return server.getSever() + ":" + server.getPort();
        }
        return null;
    }

    private String nextUserAgent() {
        String path;
        switch (submitInfo.getUserAgentType()) {
            case PC:
                path = AutoAnswerContext.FILE_CONF_UA_PC;
                break;
            case PHONE:
                path = AutoAnswerContext.FILE_CONF_UA_PHONE;
                break;
            case DEFAULT:
            default:
                return null;
        }
        List<String> lines = FileUtil.readUtf8Lines(new File(path));
        List<String> userAgents = lines.stream().filter(StrUtil::isNotBlank).collect(Collectors.toList());
        if (userAgents.size() == 0) {
            return null;
        }
        int index = CodePart.nextInRange(0, userAgents.size() - 1);
        return userAgents.get(index);
    }

    private void openLoading(WebDriver driver, UserSetting userSetting) {
        JavascriptExecutor executor = (JavascriptExecutor) driver;

        // 打开浏览器首页
        driver.get(userSetting.getHomeUrl());
        // 执行loading脚本
        String script = ResourceUtil.readUtf8Str(EXECUTOR_LOADING_JS);
        CodePart.onTimeout(EXECUTE_SCRIPT_AFTER_MS, false, () -> executeScriptNoneException(executor, script));

        // 打开IP查看链接
        String handle = driver.getWindowHandle();
        String ipUrl = userSetting.getIpUrl();
        if (StrUtil.isNotBlank(ipUrl)) {
            executor.executeScript("window.open('" + ipUrl + "', 'new_tab');");
            driver.switchTo().window(handle); // 回到问卷标签页
        }

        // 计算问卷间隔时间
        int segmentStart = Integer.parseInt(userSetting.getSegmentStart());
        int segmentEnd = Integer.parseInt(userSetting.getSegmentEnd());
        int segmentSeconds = CodePart.nextInRange(segmentStart, segmentEnd);
        // 刷新页面
        LocalDateTime future = LocalDateTime.now().plusSeconds(segmentSeconds);
        CodePart.onInterval(EXECUTE_SCRIPT_INTERVAL_MS, false, () -> {
            long seconds = LocalDateTimeUtil.between(LocalDateTime.now(), future).getSeconds();
            executeScriptNoneException(executor, "$('#executor-loading-time')[0].innerText=" + seconds);
            executeScriptNoneException(executor, "$('#executor-loading-index')[0].innerText=" + index);
        }, () -> LocalDateTime.now().isAfter(future));
    }

    private void openAnswer(WebDriver driver, UserSetting userSetting) throws InterruptedException {
        JavascriptExecutor executor = (JavascriptExecutor) driver;

        // 设置页面超时时间
        String pageLoadTimeoutString = AutoAnswerContext.getUserSetting().getPageLoadTimeout();
        int pageLoadTimeout = NumberUtil.parseInt(pageLoadTimeoutString);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));
        // 打开问卷
        driver.get(survey.getUrl());
        // 执行answer-box脚本
        String script = ResourceUtil.readUtf8Str(EXECUTOR_WINDOW_JS);
        CodePart.onTimeout(EXECUTE_SCRIPT_AFTER_MS, false, () -> executeScriptNoneException(executor, script));

        // 执行填写问卷
        executorAnswer(driver);

        // 计算问卷提交时间
        int submitStart = Integer.parseInt(userSetting.getSubmitStart());
        int submitEnd = Integer.parseInt(userSetting.getSubmitEnd());
        int submitSeconds = CodePart.nextInRange(submitStart, submitEnd);
        // 刷新页面
        LocalDateTime future = LocalDateTime.now().plusSeconds(submitSeconds);
        CodePart.onInterval(EXECUTE_SCRIPT_INTERVAL_MS, false, () -> {
            long seconds = LocalDateTimeUtil.between(LocalDateTime.now(), future).getSeconds();
            // 更新标题，避免在浮窗出现前更新，所以放在循环执行
            executeScriptNoneException(executor, "$('#executor-window-index')[0].innerText = '" + index + "';");
            executeScriptNoneException(executor, "$('#executor-window-time')[0].innerText = '" + seconds + "';");
        }, () -> LocalDateTime.now().isAfter(future));
    }

    private void executorAnswer(WebDriver driver) throws InterruptedException {
        JavascriptExecutor executor = (JavascriptExecutor) driver;

        // 执行submit脚本
        String script = this.script.getExecutorScript(); // 填写问卷脚本
        CodePart.sleep(EXECUTE_SCRIPT_AFTER_MS);
        Object obj = executor.executeScript(script, JSON.toJSONString(survey), JSON.toJSONString(subjectAnswers));
        String result = String.valueOf(obj);
        if (!"OK".equals(result)) {
            executeScriptNoneException(executor, "$('#inject-box-msg')[0].innerText = '填写问卷失败：" + result + "';");
        }
    }

    private String submitAnswer(WebDriver driver) throws InterruptedException {
        JavascriptExecutor executor = (JavascriptExecutor) driver;

        // 执行submit脚本
        String script = this.script.getSubmitScript(); // 提交脚本
        CodePart.sleep(EXECUTE_SCRIPT_AFTER_MS);
        Object obj = executor.executeScript(script, submitInfo.isAutoSubmit());
        String result = String.valueOf(obj);
        if (!"OK".equals(result)) {
            executeScriptNoneException(executor, "$('#inject-box-msg')[0].innerText = '提交第" + index + "份问卷失败：" + result + "';");
        }
        return result;
    }

    private void executeScriptNoneException(JavascriptExecutor executor, String script) {
        try {
            executor.executeScript(script);
        } catch (JavascriptException ignored) {
        }
    }

    public void onAnswerDone(Consumer<AnswerState> doneConsumer) {
        this.doneConsumer = doneConsumer;
    }
}
