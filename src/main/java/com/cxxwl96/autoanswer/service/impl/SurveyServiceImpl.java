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
import com.alibaba.fastjson.TypeReference;
import com.cxxwl96.autoanswer.context.AutoAnswerContext;
import com.cxxwl96.autoanswer.context.Script;
import com.cxxwl96.autoanswer.context.Setting;
import com.cxxwl96.autoanswer.ipproxy.CityCode;
import com.cxxwl96.autoanswer.ipproxy.IpProxy;
import com.cxxwl96.autoanswer.ipproxy.ResultBody;
import com.cxxwl96.autoanswer.model.Option;
import com.cxxwl96.autoanswer.model.Result;
import com.cxxwl96.autoanswer.model.Rule;
import com.cxxwl96.autoanswer.model.Subject;
import com.cxxwl96.autoanswer.model.SubjectAnswer;
import com.cxxwl96.autoanswer.model.SubjectType;
import com.cxxwl96.autoanswer.model.Survey;
import com.cxxwl96.autoanswer.service.SurveyService;
import com.cxxwl96.autoanswer.utils.ApplicationStore;
import com.cxxwl96.autoanswer.utils.Confirm;
import com.cxxwl96.autoanswer.utils.ExpressionUtil;
import com.cxxwl96.autoanswer.utils.RegexUtil;
import com.cxxwl96.autoanswer.utils.TextAreaLog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import netscape.javascript.JSObject;

/**
 * SurveyServiceImpl
 *
 * @author cxxwl96
 * @since 2024/3/3 01:15
 */
@Slf4j
public class SurveyServiceImpl implements SurveyService {
    // 未填写选择率
    private final static int NO_SELECTANCE = 0;

    private final TextAreaLog logger = TextAreaLog.getLogger(log);

    // 是否正在解析问卷
    private final BooleanProperty parsing = new SimpleBooleanProperty();

    // 填写规则窗口
    private Stage editRuleBrowser;

    /**
     * 解析问卷，并保存到survey
     *
     * @param url 问卷地址
     * @param domain 问卷域
     * @return 问卷ID
     */
    @Override
    public String parseSurvey(String url, String domain) {
        parsing.set(true);
        String id = LocalDateTimeUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss") + "_" + domain + "_"
            + RegexUtil.randomString(6);
        logger.info(String.format("[解析问卷][%s][%s]", url, id));
        Script script = AutoAnswerContext.getScript(domain);
        Stage browser = new Stage();
        WebView view = new WebView();
        WebEngine engine = view.getEngine();
        engine.getLoadWorker().stateProperty().addListener((observableValue, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                Confirm confirm = Confirm.buildConfirm()
                    .okAction(event -> browser.close())
                    .cancelAction((event -> browser.close()));
                try {
                    // 在这里执行JavaScript代码
                    Object result = engine.executeScript(script.getParser());
                    logger.info(String.format("[解析结果][%s]", result.toString()));
                    if (result instanceof String) {
                        Survey survey = JSON.parseObject(result.toString(), Survey.class);
                        Assert.notNull(survey, "JSON parse failed, result is null");
                        // 保存survey
                        survey.setId(id);
                        survey.setUrl(url);
                        survey.setDomain(domain);
                        AutoAnswerContext.saveSurvey(survey);
                        browser.setTitle("解析成功");
                        logger.success("[解析成功]");
                        confirm.info("提示", "解析成功", browser);
                    } else {
                        browser.setTitle("解析失败");
                        logger.error("[解析失败]" + result);
                        confirm.error("提示", "解析失败: " + result, browser);
                    }
                } catch (Exception exception) {
                    browser.setTitle("解析失败");
                    logger.error(
                        "[解析失败]" + exception.getMessage() + "\n" + ExceptionUtil.stacktraceToString(exception));
                    confirm.error("错误", "解析失败: " + exception.getMessage(), browser);
                } finally {
                    parsing.set(false);
                }
            }
        });

        engine.load(url);

        browser.setTitle("正在解析问卷，由于网络等原因比较慢，请稍后...");
        browser.getIcons().add(ApplicationStore.get("logo", Image.class));
        browser.initOwner(ApplicationStore.get("window", Stage.class));
        browser.initModality(Modality.APPLICATION_MODAL);
        browser.setScene(new Scene(view));
        browser.setOnCloseRequest(event -> {
            if (parsing.get()) {
                Confirm.buildConfirm()
                    .okAction(exception -> browser.close())
                    .warn("提示", "正在解析问卷，确认关闭？", browser);
                event.consume();
            }
        });
        browser.show();
        return id;
    }

    /**
     * 填写规则
     *
     * @param linkId 链接ID
     * @param count
     */
    @Override
    public void editRule(String linkId, int count) {
        Survey survey = AutoAnswerContext.getSurvey(linkId);
        List<List<SubjectAnswer>> surveyAnswers = AutoAnswerContext.getSurveyAnswers(linkId);
        if (survey == null) {
            throw new RuntimeException("链接" + linkId + "不存在");
        }
        survey.setCount(count);
        AutoAnswerContext.saveSurvey(survey); // 更新数量
        Stage browser = new Stage();
        this.editRuleBrowser = browser;
        WebView view = new WebView();
        double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
        double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
        view.setPrefWidth(screenWidth * 0.8);
        view.setPrefHeight(screenHeight * 0.8);

        WebEngine engine = view.getEngine();

        logger.info(String.format("[填写规则][%s]", linkId));

        engine.getLoadWorker().stateProperty().addListener((observableValue, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("surveyService", this); // 注入当前对象到页面
                // window.setMember("survey", JSON.toJSONString(survey)); // 注入当前对象到页面
                // window.setMember("surveyAnswers", JSON.toJSONString(surveyAnswers)); // 注入当前对象到页面

                engine.executeScript("setSurvey('" + JSON.toJSONString(survey) + "');"); // 传入数据到页面
                engine.executeScript("setSurveyAnswers('" + JSON.toJSONString(
                    Optional.ofNullable(surveyAnswers).orElse(new ArrayList<>())) + "');"); // 传入数据到页面
            }
        });

        engine.load(getClass().getResource("/views/rule.html").toExternalForm());

        browser.setTitle("填写规则");
        browser.getIcons().add(ApplicationStore.get("logo", Image.class));
        browser.initOwner(ApplicationStore.get("window", Stage.class));
        browser.initModality(Modality.APPLICATION_MODAL);
        browser.setScene(new Scene(view));
        browser.show();
    }

    /**
     * 提交问卷
     *
     * @param linkId 链接ID
     * @param assignProxy 是否指定代理
     * @param province 代理省
     * @param city 代理城市
     */
    @SneakyThrows
    @Override
    public void submit(String linkId, boolean assignProxy, String province, String city) {
        Survey survey = AutoAnswerContext.getSurvey(linkId);
        if (survey == null) {
            throw new RuntimeException("链接" + linkId + "不存在");
        }
        List<List<SubjectAnswer>> surveyAnswers = AutoAnswerContext.getSurveyAnswers(linkId);
        if (surveyAnswers == null) {
            throw new RuntimeException("链接" + linkId + "未保存规则，请先编辑规则");
        }
        Result<List<String>> checkResult = checkSurveyAnswers(surveyAnswers);
        // Assert.isTrue(checkResult.isSuccess(),
        //     "根据规则生成答案时存在以下题目答案未生成，请检查规则是否符合预期？\n" + StrUtil.join("\n",
        //         checkResult.getData()));

        asyncSchedule(survey, surveyAnswers, assignProxy, province, city);
    }

    /**
     * 保存问卷及答案。提供给js调用
     *
     * @param surveyStr 问卷JSON串
     * @param surveyAnswersStr 问卷答案JSON串
     */
    @Override
    public String saveSurvey(String surveyStr, String surveyAnswersStr) {
        Survey survey;
        try {
            survey = JSON.parseObject(surveyStr, Survey.class);
            AutoAnswerContext.saveSurvey(survey);

            TypeReference<List<List<SubjectAnswer>>> type = new TypeReference<List<List<SubjectAnswer>>>() {
            };
            List<List<SubjectAnswer>> surveyAnswers = JSON.parseObject(surveyAnswersStr, type);
            AutoAnswerContext.saveSurveyAnswers(survey.getId(), surveyAnswers);

            logger.success("[保存规则及答案]保存成功");
            return Result.success().toJSONString();
        } catch (Exception exception) {
            logger.error("[保存失败]" + exception.getMessage() + "\n" + ExceptionUtil.stacktraceToString(exception));
            return Result.failed("保存失败: " + exception.getMessage()).toJSONString();
        }
    }

    /**
     * 生成答案
     *
     * @param surveyStr 问卷JSON串
     */
    @Override
    public String generateAnswers(String surveyStr) {
        long start = System.currentTimeMillis();
        try {
            Survey survey = JSON.parseObject(surveyStr, Survey.class);
            Assert.isTrue(survey.getCount() > 0, "survey count should be greater than 0");
            List<List<SubjectAnswer>> surveyAnswers = dealGenerateAnswers(survey);
            logger.info(String.format("[生成答案][%s]", JSON.toJSONString(surveyAnswers)));
            logger.success("[生成答案]生成答案成功");
            return Result.success(surveyAnswers).toJSONString();
        } catch (Exception exception) {
            logger.error("[生成答案]" + exception.getMessage() + "\n" + ExceptionUtil.stacktraceToString(exception));
            return Result.failed("生成答案失败: " + exception.getMessage()).toJSONString();
        } finally {
            logger.info("[生成答案]耗时：" + (System.currentTimeMillis() - start) + "ms");
        }
    }

    /**
     * 检查答案是否填写完成。提供给js调用
     *
     * @param surveyAnswersStr 问卷答案JSON串
     */
    @Override
    public String checkAnswers(String surveyAnswersStr) {
        try {
            TypeReference<List<List<SubjectAnswer>>> type = new TypeReference<List<List<SubjectAnswer>>>() {
            };
            List<List<SubjectAnswer>> surveyAnswers = JSON.parseObject(surveyAnswersStr, type);

            return checkSurveyAnswers(surveyAnswers).toJSONString();
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            return Result.failed(exception.getMessage()).toJSONString();
        }

    }

    private Result<List<String>> checkSurveyAnswers(List<List<SubjectAnswer>> surveyAnswers) {
        List<String> errorList = new ArrayList<>();
        for (int i = 0; i < surveyAnswers.size(); i++) {
            for (SubjectAnswer subjectAnswer : surveyAnswers.get(i)) {
                if (CollUtil.isEmpty(subjectAnswer.getAnswers())) {
                    errorList.add("第" + (i + 1) + "份 第" + subjectAnswer.getNo() + "题");
                }
            }
        }
        if (errorList.size() > 0) {
            return Result.failed(Result.CODE_FAILED, "检查失败", errorList);
        }
        return Result.success();
    }

    private void asyncSchedule(Survey survey, List<List<SubjectAnswer>> surveyAnswers, boolean assignProxy,
        String province, String city) {
        Setting setting = AutoAnswerContext.getSetting();
        int segment = setting.getScheduleSegment(); // 阶段数
        long segmentSleep = setting.getScheduleSegmentSleep();
        int nThread = setting.getScheduleNThread(); // 每个阶段最大线程数
        long nThreadSleep = setting.getScheduleNThreadSleep();
        int totalSize = surveyAnswers.size(); // 任务总数

        int marginNum = totalSize % segment; // 余量
        int index = 0;
        for (int i = 0; i < segment; i++, marginNum--) {
            int segmentSize = totalSize / segment; // 每个阶段执行的任务数
            if (marginNum > 0) {
                segmentSize++;
            }
            if (segmentSize <= 0) {
                continue;
            }

            // 执行任务
            ExecutorService executor = Executors.newFixedThreadPool(nThread);
            int j;
            for (j = index; j < index + segmentSize; j++) {
                List<SubjectAnswer> subjectAnswers = surveyAnswers.get(j);
                int finalJ = j;
                int finalEnd = index + segmentSize;
                executor.submit(() -> {
                    Platform.runLater(() -> {
                        openBrowserAndSubmit(subjectAnswers, finalJ + 1, survey, assignProxy, province, city);
                    });
                    if (finalJ < finalEnd) {
                        sleep(nThreadSleep);
                    }
                });
            }
            index = j;
            executor.shutdown();

            // 间隔时间
            if (i < segment - 1) {
                sleep(segmentSleep);
            }
        }
    }

    private synchronized void openBrowserAndSubmit(List<SubjectAnswer> subjectAnswers, int index, Survey survey,
        boolean assignProxy, String province, String city) {

        logger.info("执行任务：" + index);
        // 获取提交答案脚本
        Script script = AutoAnswerContext.getScript(survey.getDomain());
        // 设置代理
        setProxy(assignProxy, province, city);

        Stage browser = new Stage();
        WebView view = new WebView();
        WebEngine engine = view.getEngine();
        engine.getLoadWorker().stateProperty().addListener((observableValue, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // 在这里执行JavaScript代码
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("subjectAnswers", JSON.toJSONString(subjectAnswers)); // 注入答案对象到页面
                try {
                    logger.info(String.format("[提交答案 %d]", index));
                    Object result = engine.executeScript(script.getExecutor());
                    logger.info(String.format("[提交结果 %d][%s]", index, result.toString()));
                    if ("OK".equals(result.toString())) {
                        logger.success(String.format("[答题成功] 第%d份", index));
                        // 关闭
                        logger.warn("关闭浏览器");
                        browser.close();
                    } else {
                        logger.error(String.format("[答题失败] 第%d份 %s", index, result));
                    }
                } catch (Exception exception) {
                    browser.setTitle("答题失败 第" + index + "份");
                    String msg = String.format("[答题失败 %d]%s\n%s", index, exception.getMessage(),
                        ExceptionUtil.stacktraceToString(exception));
                    logger.error(msg);
                }
            }
        });

        engine.load(survey.getUrl());

        browser.setTitle("自动答题 第" + index + "份");
        browser.getIcons().add(ApplicationStore.get("logo", Image.class));
        // browser.initOwner(ApplicationStore.get("window", Stage.class));
        // browser.initModality(Modality.APPLICATION_MODAL);
        browser.setScene(new Scene(view));
        browser.show();
    }

    @SneakyThrows
    private void sleep(long timeout) {
        if (timeout > 0) {
            Thread.sleep(timeout);
        }
    }

    private void setProxy(boolean assignProxy, String province, String city) {
        if (assignProxy) {
            Assert.isTrue(StrUtil.isNotBlank(province) && StrUtil.isNotBlank(city));
        } else {
            // 随机代理
            List<String> provinces = CityCode.getProvinces();
            int i = new Random(System.currentTimeMillis()).nextInt(provinces.size());
            province = provinces.get(i); // 随机省
            List<String> cities = CityCode.getCities(province);
            int j = new Random(System.currentTimeMillis()).nextInt(cities.size());
            city = cities.get(j); // 随机城市
        }
        ResultBody result = IpProxy.proxy(province, city, 1);
        if (result.getStatus() != 0) {
            logger.error(
                "代理失败：" + Optional.ofNullable(result.getInfo()).filter(StrUtil::isNotBlank).orElse("请求失败"));
            return;
        }
        ResultBody.Server server = Optional.ofNullable(result.getList())
            .filter(list -> !list.isEmpty())
            .map(list -> list.get(0))
            .orElseThrow(() -> new RuntimeException("代理失败"));
        // 设置代理
        System.setProperty("http.proxyHost", server.getSever());
        System.setProperty("http.proxyPort", String.valueOf(server.getPort()));
        System.setProperty("https.proxyHost", server.getSever());
        System.setProperty("https.proxyPort", String.valueOf(server.getPort()));
    }

    private void clearProxy() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
    }

    private List<List<SubjectAnswer>> dealGenerateAnswers(Survey survey) {

        // 初始化问卷答案
        List<List<SubjectAnswer>> surveyAnswers = initSurveyAnswers(survey);

        for (int i = 0; i < survey.getSubjects().size(); i++) { // 每个题目
            Subject subject = survey.getSubjects().get(i); // 题目
            for (Option option : subject.getOptions()) { // 每个选项

                // 能选择的问卷题。key=题目编号-选项编号-[条件]-选择率，value=题目答案
                Map<String, List<SubjectAnswer>> canSelectSubjectMap = new HashMap<>();

                for (List<SubjectAnswer> surveyAnswer : surveyAnswers) { // 每份问卷
                    SubjectAnswer subjectAnswer = surveyAnswer.get(i); // 题目答案
                    // 获取当前问卷当前选项的选择率、以及能选择的问卷题目有哪些
                    List<Rule> rules = option.getRules();
                    int selectance = option.getDefaultSelectance(); // 选择率
                    if (rulesIsEmpty(rules)) {
                        String key = String.format("%s-%s-%d", subject.getNo(), option.getTab(), selectance);
                        addCanSelectSubject(subject, canSelectSubjectMap, key, subjectAnswer);
                    } else {
                        boolean flag = false;
                        for (Rule rule : rules) {
                            if (ExpressionUtil.parseExpression(rule.getCondition(), surveyAnswer)) {
                                selectance = rule.getSelectance();
                                String key = String.format("%s-%s-%d-%s", subject.getNo(), option.getTab(), selectance,
                                    rule.getCondition());
                                addCanSelectSubject(subject, canSelectSubjectMap, key, subjectAnswer);
                                flag = true;
                                break;
                            }
                        }
                        if (!flag) {
                            String key = String.format("%s-%s-%d", subject.getNo(), option.getTab(), selectance);
                            addCanSelectSubject(subject, canSelectSubjectMap, key, subjectAnswer);
                        }
                    }
                }
                log.info("CanSelectSubjectMap: {}", canSelectSubjectMap);

                // 填写答案
                for (String key : canSelectSubjectMap.keySet()) {
                    String[] split = key.split("-");
                    String tab = split[1]; // 选项
                    int selectance = parseSelectance(split[2]); // 选择率

                    List<SubjectAnswer> canSelectSubjectAnswers = canSelectSubjectMap.get(key);
                    int finalCanSelectCount; // 最终选择个数
                    if (i == 0 || split.length == 3) {
                        // 第一个题没有条件，能选择的个数为总个数
                        // 使用默认规则的，能选择的个数为总个数
                        finalCanSelectCount = surveyAnswers.size() * selectance / 100;
                    } else {
                        finalCanSelectCount = canSelectSubjectAnswers.size() * selectance / 100;
                    }
                    // 打乱canSelectSubjectAnswers顺序
                    shuffleCanSelectSubjectAnswers(canSelectSubjectAnswers);

                    for (int j = 0; j < finalCanSelectCount && j < canSelectSubjectAnswers.size(); j++) {
                        canSelectSubjectAnswers.get(j).getAnswers().add(tab);
                    }
                }
            }
        }
        log.info("SurveyAnswers: {}", JSON.toJSONString(surveyAnswers));
        return surveyAnswers;
    }

    private void shuffleCanSelectSubjectAnswers(List<SubjectAnswer> list) {
        int index = list.size() - 1;
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < list.size(); i++) {
            int preIndex = random.nextInt(index + 1);
            SubjectAnswer tmp = list.get(preIndex);
            list.set(preIndex, list.get(index));
            list.set(index, tmp);
            index--;
        }
    }

    private void addCanSelectSubject(Subject subject, Map<String, List<SubjectAnswer>> canSelectSubjectMap, String key,
        SubjectAnswer subjectAnswer) {
        List<SubjectAnswer> canSelectSubjects;
        if (canSelectSubjectMap.containsKey(key)) {
            canSelectSubjects = canSelectSubjectMap.get(key);
        } else {
            canSelectSubjects = new ArrayList<>();
            canSelectSubjectMap.put(key, canSelectSubjects);
        }
        switch (subject.getType()) {
            case SubjectType.SINGLE_CHOICE: // 单选
            case SubjectType.GAP_FILLING: // 填空
                if (subjectAnswer.getAnswers().isEmpty()) { // 没有选择的单选才能添加到能选择列表中
                    canSelectSubjects.add(subjectAnswer);
                }
                break;
            default:
                canSelectSubjects.add(subjectAnswer);
        }
    }

    private boolean rulesIsEmpty(List<Rule> rules) {
        return CollUtil.isEmpty(rules) || rules.stream().allMatch(rule -> rule.getSelectance() == NO_SELECTANCE);
    }

    private int parseSelectance(String selectance) {
        if (StrUtil.isBlank(selectance)) {
            return NO_SELECTANCE;
        }
        if (NumberUtil.isInteger(selectance)) {
            int value = Integer.parseInt(selectance);
            if (value < 0 || value > 100) {
                throw new RuntimeException("错误的选择率：" + selectance + "，选择率只能处于0-100");
            }
            return value;
        }
        throw new RuntimeException("错误的选择率：" + selectance + "，选择率只能处于0-100之间的数字");
    }

    private List<List<SubjectAnswer>> initSurveyAnswers(Survey survey) {
        List<List<SubjectAnswer>> surveyAnswers = new ArrayList<>(); // 所有问卷答案
        for (int i = 0; i < survey.getCount(); i++) {
            List<SubjectAnswer> subjectAnswers = new ArrayList<>();  // 当前问卷所有题目答案
            List<Subject> subjects = survey.getSubjects();
            for (Subject subject : subjects) {
                SubjectAnswer subjectAnswer = new SubjectAnswer(); // 题目答案
                subjectAnswer.setNo(subject.getNo());
                subjectAnswer.setAnswers(new ArrayList<>());
                subjectAnswers.add(subjectAnswer);
            }
            surveyAnswers.add(subjectAnswers);
        }
        return surveyAnswers;
    }
}


