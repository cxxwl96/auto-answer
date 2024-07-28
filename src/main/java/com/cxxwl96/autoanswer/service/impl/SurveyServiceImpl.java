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

import com.cxxwl96.autoanswer.context.AutoAnswerContext;
import com.cxxwl96.autoanswer.enums.AnswerState;
import com.cxxwl96.autoanswer.enums.ProxyType;
import com.cxxwl96.autoanswer.model.Result;
import com.cxxwl96.autoanswer.model.Subject;
import com.cxxwl96.autoanswer.model.SubjectType;
import com.cxxwl96.autoanswer.model.SubmitInfo;
import com.cxxwl96.autoanswer.model.Survey;
import com.cxxwl96.autoanswer.service.SurveyService;
import com.cxxwl96.autoanswer.utils.CodePart;
import com.cxxwl96.autoanswer.utils.SeleniumUtil;
import com.cxxwl96.autoanswer.utils.TextAreaLog;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * SurveyServiceImpl
 *
 * @author cxxwl96
 * @since 2024/3/3 01:15
 */
@Slf4j
public class SurveyServiceImpl implements SurveyService {
    private final TextAreaLog logger = TextAreaLog.getLogger(log);

    // 答题线程池
    private ExecutorService answerExecutor;

    // 监控答题是否完成线程池
    private final ExecutorService monitorDoneExecutor = Executors.newSingleThreadExecutor();

    // 执行器状态
    private final Map<SurveyExecutor, AnswerState> executorStateMap = new ConcurrentHashMap<>();

    // 执行器终态状态
    private static final List<AnswerState> DONE_STATE_LIST = new ArrayList<AnswerState>() {
        {
            add(AnswerState.SUCCESS);
            add(AnswerState.FAILED);
            add(AnswerState.STOP);
        }
    };

    // 线程个数
    private int threadSize;

    // 提交信息
    private SubmitInfo submitInfo;

    public SurveyServiceImpl() {
        updateAnswerExecutor(); // 更新线程池
    }

    /**
     * 提交问卷
     *
     * @param model 提交信息
     */
    @Override
    public void submit(SubmitInfo model) {
        submitInfo = model;
        // 参数校验
        validate(submitInfo);
        // 解析xlsx
        submitInfo.setSurveys(parseSurveys(submitInfo.getXlsxPath()));
        log.info("Submit: {}", submitInfo);
        AutoAnswerContext.saveSubmitInfo(submitInfo);
        // 更新线程池
        updateAnswerExecutor();

        executorStateMap.clear();

        // 提交任务
        String message = String.format("共%d份问卷", submitInfo.getSurveys().size());
        logger.warn("[开始答题]" + message);
        for (Survey survey : submitInfo.getSurveys()) {
            SurveyExecutor task = new SurveyExecutor(submitInfo, survey);
            task.onAnswerDone((state) -> executorStateMap.put(task, state));
            answerExecutor.submit(task);
            executorStateMap.put(task, AnswerState.INIT);
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
            for (Map.Entry<SurveyExecutor, AnswerState> entry : executorStateMap.entrySet()) {
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

    private void validate(SubmitInfo submitInfo) {
        Assert.isTrue(this.answerAllDone(), "答题任务未执行完");
        Assert.notBlank(submitInfo.getDomain(), "请选择域");
        Assert.notBlank(submitInfo.getUrl(), "请输入链接");
        Assert.notBlank(submitInfo.getXlsxPath(), "请选择xlsx文件");
        File xlsxFile = new File(submitInfo.getXlsxPath());
        Assert.isTrue(xlsxFile.exists(), "请选择xlsx文件");
        Assert.isTrue(xlsxFile.getName().endsWith(".xlsx"), "非法文件 " + xlsxFile.getName());
        if (submitInfo.getProxyType() == ProxyType.ASSIGN_PROXY) {
            Assert.isTrue(StrUtil.isNotBlank(submitInfo.getProvince()) && StrUtil.isNotBlank(submitInfo.getCity()), "请选择代理城市");
        }
        String binary = AutoAnswerContext.getUserSetting().getChromeAddress();
        log.info("ChromeBinary: {}", binary);
        Assert.notBlank(binary, "请先设置浏览器路径");
    }

    private List<Survey> parseSurveys(String xlsxPath) {
        ExcelReader reader = ExcelUtil.getReader(xlsxPath, "answer");
        List<List<Object>> lists = reader.read();
        if (lists == null || lists.size() < 3) {
            return new ArrayList<>();
        }
        // 获取题目序号及题目类型
        List<Map.Entry<String, SubjectType>> noTypes = new ArrayList<>();
        for (int i = 1; i < lists.get(0).size(); i++) { // 从第2列解析
            Object noObj = lists.get(0).get(i); // 题目序号
            Object typeObj = lists.get(1).get(i); // 题目类型
            Assert.isFalse(StrUtil.isBlankIfStr(noObj), "单元格格式错误：" + transformCell(i, 0));
            Assert.isFalse(StrUtil.isBlankIfStr(typeObj), "单元格格式错误：" + transformCell(i, 1));
            SubjectType subjectType = SubjectType.getByName(typeObj.toString());
            Assert.notNull(subjectType, "单元格格式错误：" + transformCell(i, 1));
            noTypes.add(new AbstractMap.SimpleEntry<>(noObj.toString(), subjectType));
        }

        List<Survey> surveys = new ArrayList<>();

        for (int i = 2; i < lists.size(); i++) {
            List<Subject> subjects = new ArrayList<>();
            for (int j = 1; j < lists.get(i).size(); j++) {
                if (j - 1 < noTypes.size()) {
                    Object answer = lists.get(i).get(j);
                    Subject subject = new Subject();
                    subject.setNo(noTypes.get(j - 1).getKey());
                    subject.setType(noTypes.get(j - 1).getValue());
                    subject.setAnswer(StrUtil.isBlankIfStr(answer) ? StrUtil.EMPTY : answer.toString());
                    subjects.add(subject);
                } else {
                    break;
                }
            }
            if (!subjects.isEmpty()) {
                Survey survey = new Survey();
                survey.setName(StrUtil.isBlankIfStr(lists.get(i).get(0)) ? StrUtil.EMPTY : lists.get(i).get(0).toString());
                survey.setSubjects(subjects);
                surveys.add(survey);
            }
        }
        return surveys;
    }

    private static String transformCell(int x, int y) {
        return ((char) ('A' + x)) + ":" + (y + 1);
    }

    /**
     * 是否答题完成
     *
     * @return 是否答题完成
     */
    @Override
    public boolean answerAllDone() {
        for (Map.Entry<SurveyExecutor, AnswerState> entry : executorStateMap.entrySet()) {
            if (!DONE_STATE_LIST.contains(entry.getValue())) {
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
                logger.warn("[提交问卷]停止" + task.getSurvey().getName());
                executorStateMap.put(task, AnswerState.STOP);
            }
            answerExecutor = Executors.newFixedThreadPool(threadSize);
            SeleniumUtil.quitAll();
        }).start();
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