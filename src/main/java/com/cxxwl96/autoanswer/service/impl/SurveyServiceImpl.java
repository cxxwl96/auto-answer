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
import com.cxxwl96.autoanswer.enums.AnswerState;
import com.cxxwl96.autoanswer.enums.ProxyType;
import com.cxxwl96.autoanswer.model.Option;
import com.cxxwl96.autoanswer.model.Result;
import com.cxxwl96.autoanswer.model.Rule;
import com.cxxwl96.autoanswer.model.Subject;
import com.cxxwl96.autoanswer.model.SubjectAnswer;
import com.cxxwl96.autoanswer.model.SubjectType;
import com.cxxwl96.autoanswer.model.SubmitInfo;
import com.cxxwl96.autoanswer.model.Survey;
import com.cxxwl96.autoanswer.service.SurveyService;
import com.cxxwl96.autoanswer.utils.ApplicationStore;
import com.cxxwl96.autoanswer.utils.CodePart;
import com.cxxwl96.autoanswer.utils.Confirm;
import com.cxxwl96.autoanswer.utils.ExpressionUtil;
import com.cxxwl96.autoanswer.utils.RegexUtil;
import com.cxxwl96.autoanswer.utils.SeleniumUtil;
import com.cxxwl96.autoanswer.utils.TextAreaLog;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
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

    // 答题线程池
    private ExecutorService answerExecutor;

    // 监控答题是否完成线程池
    private final ExecutorService monitorDoneExecutor;

    // 问卷答题状态
    private final Map<SurveyExecutor, AnswerState> answerStateMap = new ConcurrentHashMap<>();

    // 线程个数
    private int threadSize;

    // 问卷份数
    private int answerSize;

    public SurveyServiceImpl() {
        updateAnswerExecutor(); // 更新线程池
        monitorDoneExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * 解析问卷，并保存到survey
     *
     * @param url 问卷地址
     * @param domain 问卷域
     * @return 问卷ID
     */
    @Override
    public String parseSurvey(String domain, String url) {
        Assert.isTrue(this.answerAllDone(), "答题任务未执行完");
        Assert.notBlank(domain, "请选择域");
        Assert.notBlank(url, "请输入链接");
        Assert.isTrue(url.matches("^https?://.+$"), "无效的链接");
        parsing.set(true);
        String id = LocalDateTimeUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss") + "_" + domain + "_" + RegexUtil.randomString(6);
        logger.info(String.format("[解析问卷][%s][%s]", url, id));
        Script script = AutoAnswerContext.getScript(domain);
        Stage browser = new Stage();
        WebView view = new WebView();
        WebEngine engine = view.getEngine();
        engine.getLoadWorker().stateProperty().addListener((observableValue, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                Confirm confirm = Confirm.buildConfirm().okAction(event -> browser.close()).cancelAction((event -> browser.close()));
                try {
                    // 在这里执行JavaScript代码
                    Object result = engine.executeScript(script.getParserScript());
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
                    logger.error("[解析失败]" + exception.getMessage() + "\n" + ExceptionUtil.stacktraceToString(exception));
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
                Confirm.buildConfirm().okAction(exception -> browser.close()).warn("提示", "正在解析问卷，确认关闭？", browser);
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
     */
    @Override
    public void editRule(String linkId) {
        Assert.isTrue(this.answerAllDone(), "答题任务未执行完");
        Assert.notBlank(linkId, "请输入链接ID");
        Assert.notBlank(linkId, "请输入问卷数");

        Survey survey = AutoAnswerContext.getSurvey(linkId);
        List<List<SubjectAnswer>> surveyAnswers = AutoAnswerContext.getSurveyAnswers(linkId);
        Assert.notNull(survey, "链接" + linkId + "不存在");
        Stage browser = new Stage();
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

                engine.executeScript("setSurvey('" + JSON.toJSONString(survey) + "');"); // 传入数据到页面
                engine.executeScript("setSurveyAnswers('" + JSON.toJSONString(Optional.ofNullable(surveyAnswers).orElse(new ArrayList<>()))
                    + "');"); // 传入数据到页面
            }
        });

        engine.load(getClass().getResource("/views/Rule.html").toExternalForm());

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
     * @param domain 问卷域
     * @param linkId 链接ID
     * @param submitInfo 代理信息
     */
    @Override
    public void submit(String domain, String linkId, SubmitInfo submitInfo) {
        Assert.isTrue(this.answerAllDone(), "答题任务未执行完");

        Assert.notBlank(domain, "请选择域");
        Assert.notBlank(linkId, "请输入链接ID");

        Survey survey = AutoAnswerContext.getSurvey(linkId);
        Assert.notNull(survey, "链接" + linkId + "不存在");

        List<List<SubjectAnswer>> surveyAnswers = AutoAnswerContext.getSurveyAnswers(linkId);
        Assert.notNull(surveyAnswers, "链接" + linkId + "未保存规则，请先编辑规则");

        if (submitInfo.getProxyType() == ProxyType.ASSIGN_PROXY) {
            Assert.isTrue(StrUtil.isNotBlank(submitInfo.getProvince()) && StrUtil.isNotBlank(submitInfo.getCity()), "请选择代理城市");
        }

        String binary = AutoAnswerContext.getUserSetting().getChromeAddress();
        log.info("ChromeBinary: {}", binary);
        Assert.notBlank(binary, "请先设置浏览器路径");

        Result<List<String>> checkResult = checkSurveyAnswers(survey, surveyAnswers);
        Assert.isTrue(checkResult.isSuccess(),
            "根据规则生成答案时存在以下题目答案未生成，请检查规则是否符合预期？\n" + StrUtil.join("\n", checkResult.getData()));

        updateAnswerExecutor(); // 更新线程池
        Script script = AutoAnswerContext.getScript(domain);
        // 提交线程
        answerSize = surveyAnswers.size();
        answerStateMap.clear();
        String message = String.format("链接ID：%s，共%d份", survey.getId(), answerSize);

        logger.warn("[开始答题]" + message);
        for (int i = 0; i < answerSize; i++) {
            List<SubjectAnswer> subjectAnswers = surveyAnswers.get(i);
            SurveyExecutor task = new SurveyExecutor(survey, subjectAnswers, i + 1, script, submitInfo);
            task.onAnswerDone((state) -> answerStateMap.put(task, state));
            answerExecutor.submit(task);
            answerStateMap.put(task, AnswerState.INIT);
        }

        // 监控是否完成
        monitorDoneExecutor.execute(() -> {
            while (!this.answerAllDone()) {
                CodePart.sleepNoneException(200);
            }
            // 统计
            int success = 0;
            int failed = 0;
            int cancelled = 0;
            for (Map.Entry<SurveyExecutor, AnswerState> entry : answerStateMap.entrySet()) {
                switch (entry.getValue()) {
                    case SUCCESS:
                        success++;
                        break;
                    case FAILED:
                        failed++;
                        break;
                    case STOP:
                        cancelled++;
                        break;
                }
            }
            String result = String.format("[结束答题]%s，成功：%d，失败：%d，停止：%d", message, success, failed, cancelled);
            logger.warn(result);
        });
    }

    /**
     * 是否答题完成
     *
     * @return 是否答题完成
     */
    @Override
    public boolean answerAllDone() {
        if (answerStateMap.size() != answerSize) {
            return false;
        }
        List<AnswerState> doneStateList = new ArrayList<AnswerState>() {
            {
                add(AnswerState.SUCCESS);
                add(AnswerState.FAILED);
                add(AnswerState.STOP);
            }
        };
        for (Map.Entry<SurveyExecutor, AnswerState> entry : answerStateMap.entrySet()) {
            if (!doneStateList.contains(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 停止答题
     */
    @Override
    public void stop() {
        new Thread(() -> {
            if (answerAllDone()) {
                return;
            }
            logger.warn("正在停止答题...");
            List<Runnable> runnables = answerExecutor.shutdownNow();
            for (Runnable runnable : runnables) {
                FutureTask<Result<?>> future = (FutureTask<Result<?>>) runnable;
                SurveyExecutor task = (SurveyExecutor) ReflectUtil.getFieldValue(future, "callable");
                logger.warn("[提交问卷]停止第" + task.getIndex() + "份问卷");
                answerStateMap.put(task, AnswerState.STOP);
            }
            answerExecutor = Executors.newFixedThreadPool(threadSize);
            SeleniumUtil.quitAll();
        }).start();
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
     * @param surveyStr 问卷JSON串
     * @param surveyAnswersStr 问卷答案JSON串
     */
    @Override
    public String checkAnswers(String surveyStr, String surveyAnswersStr) {
        try {
            TypeReference<List<List<SubjectAnswer>>> type = new TypeReference<List<List<SubjectAnswer>>>() {
            };
            List<List<SubjectAnswer>> surveyAnswers = JSON.parseObject(surveyAnswersStr, type);

            Survey survey = JSON.parseObject(surveyStr, Survey.class);

            return checkSurveyAnswers(survey, surveyAnswers).toJSONString();
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            return Result.failed(exception.getMessage()).toJSONString();
        }

    }

    /**
     * 答案导出Excel。提供给js调用
     *
     * @param surveyStr 问卷JSON串
     * @param surveyAnswersStr 问卷答案JSON串
     */
    @Override
    public String exportExcel(String surveyStr, String surveyAnswersStr) {
        try {
            TypeReference<List<List<SubjectAnswer>>> type = new TypeReference<List<List<SubjectAnswer>>>() {
            };
            List<List<SubjectAnswer>> surveyAnswers = JSON.parseObject(surveyAnswersStr, type);

            Survey survey = JSON.parseObject(surveyStr, Survey.class);

            // 导出Excel
            Assert.notEmpty(surveyAnswers, "没有数据可以导出");
            ExcelWriter writer = ExcelUtil.getWriter();
            for (int i = 0; i < survey.getSubjects().size(); i++) {
                writer.writeCellValue(i + 1, 0, "第" + survey.getSubjects().get(i).getNo() + "题");
            }

            for (int i = 0; i < surveyAnswers.size(); i++) {
                writer.writeCellValue(0, i + 1, "第" + i + "份");
                for (int j = 0; j < surveyAnswers.get(i).size(); j++) {
                    SubjectAnswer subjectAnswer = surveyAnswers.get(i).get(j);
                    writer.writeCellValue(j + 1, i + 1, String.join(" ", subjectAnswer.getAnswers()));
                }
            }

            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("选择保存的路径");
            File directoryFile = directoryChooser.showDialog(ApplicationStore.get("window", Stage.class));
            if (directoryFile != null) {
                String pathname = directoryFile.getAbsolutePath() + "/" + survey.getId() + ".xlsx";
                writer.flush(new File(pathname));
                writer.close();
                log.info("导出Excel: {}", pathname);
                return Result.success().toJSONString();
            }
            return Result.failed("请选择保存的路径").toJSONString();
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            return Result.failed(exception.getMessage()).toJSONString();
        }
    }

    public static void main(String[] args) {
        String surveyStr = FileUtil.readUtf8String(new File(".survey/20240418205532_问卷星_Hy65WS.json"));
        String surveyAnswersStr = FileUtil.readUtf8String(new File(".survey/20240418205532_问卷星_Hy65WS_answers.json"));

        new SurveyServiceImpl().exportExcel(surveyStr, surveyAnswersStr);
    }

    private Result<List<String>> checkSurveyAnswers(Survey survey, List<List<SubjectAnswer>> surveyAnswers) {
        List<String> errorList = new ArrayList<>();
        for (int i = 0; i < surveyAnswers.size(); i++) {
            for (int j = 0; j < surveyAnswers.get(i).size(); j++) {
                SubjectAnswer subjectAnswer = surveyAnswers.get(i).get(j);
                if (survey.getSubjects().get(j).isRequire() && CollUtil.isEmpty(subjectAnswer.getAnswers())) {
                    errorList.add("第" + (i + 1) + "份 第" + subjectAnswer.getNo() + "题");
                }
            }
        }
        if (!errorList.isEmpty()) {
            return Result.failed(Result.CODE_FAILED, "检查失败", errorList);
        }
        return Result.success();
    }

    private List<List<SubjectAnswer>> dealGenerateAnswers(Survey survey) {

        // 初始化问卷答案
        List<List<SubjectAnswer>> surveyAnswers = initSurveyAnswers(survey);

        for (int i = 0; i < survey.getSubjects().size(); i++) { // 每个题目
            Subject subject = survey.getSubjects().get(i); // 题目
            for (Option option : subject.getOptions()) { // 每个选项
                // 不选择该项
                if (option.isDisabled()) {
                    continue;
                }

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

                // 填写答案
                for (String key : canSelectSubjectMap.keySet()) {
                    String[] split = key.split("-");
                    String tab = split[1]; // 选项
                    int selectance = parseSelectance(split[2]); // 选择率

                    List<SubjectAnswer> canSelectSubjectAnswers = canSelectSubjectMap.get(key);
                    double floatSelectance; // 最终选择个数
                    if (i == 0 || split.length == 3) {
                        // 第一个题没有条件，能选择的个数为总个数
                        // 使用默认规则的，能选择的个数为总个数
                        floatSelectance = surveyAnswers.size() * selectance / 100.0;
                    } else {
                        floatSelectance = canSelectSubjectAnswers.size() * selectance / 100.0;
                    }
                    int finalCanSelectCount = (int) Math.ceil(floatSelectance); // 向上取整
                    // 打乱canSelectSubjectAnswers顺序
                    shuffleList(canSelectSubjectAnswers);
                    // 优先选择没有答案的问卷
                    List<SubjectAnswer> sortedCanSelectSubjectAnswers = canSelectSubjectAnswers.stream()
                        .sorted(Comparator.comparing(subjectAnswer -> subjectAnswer.getAnswers().size()))
                        .collect(Collectors.toList());

                    for (int j = 0; j < finalCanSelectCount && j < sortedCanSelectSubjectAnswers.size(); j++) {
                        sortedCanSelectSubjectAnswers.get(j).getAnswers().add(tab);
                    }
                }
            }
        }

        // 自动补充答案
        for (int i = 0; i < surveyAnswers.size(); i++) {
            for (int j = 0; j < surveyAnswers.get(i).size(); j++) {
                Subject subject = survey.getSubjects().get(j);
                SubjectAnswer subjectAnswer = surveyAnswers.get(i).get(j);

                // 未填写答案
                List<String> answers = subjectAnswer.getAnswers();
                if (CollUtil.isEmpty(answers)) {
                    // 过滤选项
                    List<String> tabs = filterOptionTabs(subject);
                    switch (subject.getType()) {
                        case SubjectType.SINGLE_CHOICE: // 单选
                        case SubjectType.GAP_FILLING: // 填空
                        case SubjectType.MULTIPLE_CHOICE: // 多选
                            if (tabs.size() > 0) {
                                // 选择率大于0才补充
                                int i1 = tabs.get(0).charAt(0) - 'A';
                                int defaultSelectance = subject.getOptions().get(i1).getDefaultSelectance();
                                if (defaultSelectance > 0) {
                                    answers.add(tabs.get(0));
                                    subjectAnswer.getSystemAnswers().add(tabs.get(0)); // 系统自动选择的答案
                                }
                            }
                            break;
                    }
                }
                // 多选题至少选择
                int atLeast = CodePart.nextInRange(subject.getAtLeastBegin(), subject.getAtLeastEnd()); // 得到至少选择的个数
                if (SubjectType.MULTIPLE_CHOICE.equals(subject.getType()) && answers.size() < atLeast) {
                    // 过滤选项
                    List<String> tabs = filterOptionTabs(subject);
                    for (int k = 0; k < tabs.size() && answers.size() < atLeast; k++) {
                        String tab = tabs.get(k);
                        // 添加下一个选项
                        if (!answers.contains(tab)) {
                            answers.add(tab);
                            subjectAnswer.getSystemAnswers().add(tab); // 系统自动选择的答案
                        }
                    }
                }
            }
        }

        // 答案排序
        for (List<SubjectAnswer> surveyAnswer : surveyAnswers) {
            for (SubjectAnswer subjectAnswer : surveyAnswer) {
                List<String> answers = subjectAnswer.getAnswers().stream().sorted().collect(Collectors.toList());
                List<String> systemAnswers = subjectAnswer.getSystemAnswers().stream().sorted().collect(Collectors.toList());
                subjectAnswer.setAnswers(answers);
                subjectAnswer.setSystemAnswers(systemAnswers);
            }
        }
        log.info("SurveyAnswers: {}", JSON.toJSONString(surveyAnswers));
        return surveyAnswers;
    }

    private List<String> filterOptionTabs(Subject subject) {
        List<String> options = subject.getOptions()
            .stream()
            .filter(opt -> opt.getDefaultSelectance() > 0)
            .map(Option::getTab)
            .collect(Collectors.toList());
        shuffleList(options);
        return options;
    }

    private boolean allEquals(List<Integer> list) {
        if (CollUtil.isEmpty(list)) {
            return true;
        }
        Integer value = list.get(0);
        for (Integer item : list) {
            if (!item.equals(value)) {
                return false;
            }
        }
        return true;
    }

    private <T> void shuffleList(List<T> list) {
        int index = list.size() - 1;
        Random random = new Random();
        for (int i = 0; i < list.size(); i++) {
            int preIndex = random.nextInt(index + 1);
            T tmp = list.get(preIndex);
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
                subjectAnswer.setSystemAnswers(new ArrayList<>());
                subjectAnswers.add(subjectAnswer);
            }
            surveyAnswers.add(subjectAnswers);
        }
        return surveyAnswers;
    }

    private void updateAnswerExecutor() {
        String chromeCount = AutoAnswerContext.getUserSetting().getChromeCount(); // 同时打开最多浏览器个数，即线程个数
        int size = NumberUtil.parseInt(chromeCount);
        if (size != threadSize) {
            threadSize = size;
            answerExecutor = Executors.newFixedThreadPool(threadSize);
        }
    }
}


