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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.cxxwl96.autoanswer.model.SubjectAnswer;
import com.cxxwl96.autoanswer.model.Survey;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * AutoAnswerContext
 *
 * @author cxxwl96
 * @since 2024/3/1 22:55
 */
@Slf4j
public class AutoAnswerContext {
    /**
     * JS脚本
     */
    private static volatile Map<String, Script> scriptMap;

    /**
     * 设置
     */
    private static volatile Map<String, String> settingMap;

    /**
     * 问卷
     */
    private static final Map<String, Survey> SURVEY_MAP = new HashMap<>();

    /**
     * 问卷答案
     */
    private static final Map<String, List<List<SubjectAnswer>>> SURVEY_ANSWER_MAP = new HashMap<>();

    private static final Setting SETTING = new Setting();

    private static final String FILE_CONF_SCRIPTS = "conf/scripts";

    private static final String FILE_CONF_SCRIPTS_PARSER = "/parser.js";

    private static final String FILE_CONF_SCRIPTS_EXECUTOR = "/executor.js";

    private static final String FILE_CONF_SETTING = "conf/setting.properties";

    private static final String FILE_SURVEY = ".survey/";

    private static final String FILE_SURVEY_ANSWERS_SUFFIX = "_answers";

    public static Script getScript(String domain) {
        Assert.notBlank(domain, "domain must be blank");
        Script script = getScriptMap().get(domain);
        Assert.notNull(script, domain + " script is null");
        return script;
    }

    public static String getSettingValue(String key) {
        getSetting();
        return settingMap.get(key);
    }

    public static Setting getSetting() {
        if (settingMap == null) {
            synchronized (Setting.class) {
                if (settingMap == null) {
                    // 扫描设置
                    settingMap = new HashMap<>();
                    File file = new File(FILE_CONF_SETTING);
                    String text = FileUtil.readUtf8String(file);
                    Scanner scanner = new Scanner(text);
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine().trim();
                        if (line.startsWith("#")) {
                            continue;
                        }
                        int i = line.indexOf("=");
                        if (i < 0) {
                            continue;
                        }
                        String key = line.substring(0, i).trim();
                        int endIndex = line.indexOf("#");
                        String value = line.substring(i + 1, endIndex > 0 ? endIndex : line.length()).trim();
                        settingMap.put(key, value);
                    }
                    // 注入Setting
                    Field[] fields = Setting.class.getDeclaredFields();
                    for (Field field : fields) {
                        if (!field.isAnnotationPresent(Value.class)) {
                            continue;
                        }
                        Value annotation = field.getAnnotation(Value.class);
                        String key = annotation.value();
                        if (!StrUtil.isNotBlank(key)) {
                            continue;
                        }
                        String value = settingMap.get(key);
                        Assert.notBlank(value, key + " not setting");
                        ReflectUtil.setFieldValue(SETTING, field, value);
                    }
                }
            }
        }
        return SETTING;
    }

    public static void saveSurvey(Survey survey) {
        SURVEY_MAP.put(survey.getId(), survey);
        FileUtil.writeUtf8String(JSON.toJSONString(survey), new File(FILE_SURVEY + survey.getId() + ".json"));
    }

    public static Survey getSurvey(String id) {
        Survey survey = SURVEY_MAP.get(id);
        if (survey == null) {
            File file = new File(FILE_SURVEY + id + ".json");
            if (file.exists()) {
                try {
                    String read = FileUtil.readUtf8String(file);
                    return JSON.parseObject(read, Survey.class);
                } catch (Exception exception) {
                    log.error(exception.getMessage(), exception);
                }
            }
        }
        return survey;
    }

    public static void saveSurveyAnswers(String id, List<List<SubjectAnswer>> surveyAnswers) {
        String filename = id + FILE_SURVEY_ANSWERS_SUFFIX;
        SURVEY_ANSWER_MAP.put(filename, surveyAnswers);
        FileUtil.writeUtf8String(JSON.toJSONString(surveyAnswers), new File(FILE_SURVEY + filename + ".json"));
    }

    public static List<List<SubjectAnswer>> getSurveyAnswers(String id) {
        String filename = id + FILE_SURVEY_ANSWERS_SUFFIX;
        List<List<SubjectAnswer>> surveyAnswers = SURVEY_ANSWER_MAP.get(filename);
        if (surveyAnswers == null) {
            File file = new File(FILE_SURVEY + filename + ".json");
            if (file.exists()) {
                try {
                    String read = FileUtil.readUtf8String(file);
                    return JSON.parseObject(read, new TypeReference<List<List<SubjectAnswer>>>() {
                    });
                } catch (Exception exception) {
                    log.error(exception.getMessage(), exception);
                }
            }
        }
        return surveyAnswers;
    }

    public static void removeSurveyAnswers(String id) {
        String filename = id + FILE_SURVEY_ANSWERS_SUFFIX;
        SURVEY_ANSWER_MAP.remove(filename);
        File file = new File(filename);
        if (file.exists()) {
            try {
                FileUtil.del(file);
            } catch (IORuntimeException exception) {
                log.error(exception.getMessage(), exception);
                FileUtil.writeUtf8String(StrUtil.EMPTY, file);
            }
        }
    }

    public static Map<String, Script> getScriptMap() {
        if (scriptMap == null) {
            synchronized (AutoAnswerContext.class) {
                if (scriptMap == null) {
                    scriptMap = new HashMap<>();
                    File scriptsFile = new File(FILE_CONF_SCRIPTS);
                    File[] files = scriptsFile.listFiles();
                    if (files == null) {
                        return scriptMap;
                    }
                    for (File domainFile : files) {
                        String domain = domainFile.getName();
                        String domainPath;
                        try {
                            domainPath = domainFile.getCanonicalPath();
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                        String parserScript = FileUtil.readUtf8String(domainPath + FILE_CONF_SCRIPTS_PARSER);
                        String executorScript = FileUtil.readUtf8String(domainPath + FILE_CONF_SCRIPTS_EXECUTOR);

                        Assert.notBlank(parserScript, "parser.js script must be blank");
                        Assert.notBlank(executorScript, "executor.js script must be blank");

                        Script script = new Script();
                        script.setParser(parserScript);
                        script.setExecutor(executorScript);

                        scriptMap.put(domain, script);
                    }
                }
            }
        }
        return scriptMap;
    }
}
