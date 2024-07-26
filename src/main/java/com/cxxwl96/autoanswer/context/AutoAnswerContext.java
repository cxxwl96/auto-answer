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
import java.util.Optional;
import java.util.Scanner;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
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
     * 用户设置
     */
    private static volatile UserSetting userSetting;

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

    private static final String FILE_CONF_SCRIPTS_SUBMIT = "/submit.js";

    private static final String FILE_CONF_SETTING = "conf/setting.properties";

    public static final String FILE_CONF_UA_PC = "conf/电脑ua.txt";

    public static final String FILE_CONF_UA_PHONE = "conf/手机ua.txt";

    // 用户设置
    private static final String FILE_CONF_USER_SETTING = SystemUtil.getUserInfo().getHomeDir() + "/.auto_answer";

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

                    // 初始化WebDriver配置
                    OsInfo osInfo = SystemUtil.getOsInfo();
                    if (osInfo.isWindows()) {
                        SETTING.setDriver(SETTING.getWinDriver());
                        SETTING.setBinary(SETTING.getWinBinary());
                    } else if (osInfo.isMac()) {
                        SETTING.setDriver(SETTING.getMacDriver());
                        SETTING.setBinary(SETTING.getMacBinary());
                    } else {
                        String error = String.format("不支持的应用系统：%s, 系统版本：%s", osInfo.getName(), osInfo.getVersion());
                        throw new RuntimeException(error);
                    }
                }
            }
        }
        return SETTING;
    }

    public static UserSetting getUserSetting() {
        if (userSetting == null) {
            synchronized (UserSetting.class) {
                if (userSetting == null) {
                    File file = new File(FILE_CONF_USER_SETTING);
                    if (file.exists()) {
                        String read = FileUtil.readUtf8String(file);
                        String oldVersion = Optional.ofNullable(JSON.parseObject(read))
                            .map(obj -> obj.get("version"))
                            .map(Object::toString)
                            .orElse(null);
                        String nowVersion = new UserSetting().getVersion();
                        if (!StrUtil.equals(oldVersion, nowVersion)) {
                            // 版本不一致则删除用户设置
                            FileUtil.del(file);
                        }
                    }
                    Setting setting = getSetting();
                    if (!file.exists()) {
                        FileUtil.touch(file);
                        userSetting = new UserSetting();
                        // 获取WebDriver默认设置
                        Assert.isTrue(new File(setting.getDriver()).exists(),
                            () -> new RuntimeException("Driver file not found: " + setting.getDriver()));
                        Assert.isTrue(new File(setting.getBinary()).exists(),
                            () -> new RuntimeException("Binary file not found: " + setting.getBinary()));
                        userSetting.setChromeAddress(setting.getBinary()); // 设置chrome
                        // 保存用户设置
                        FileUtil.writeUtf8String(JSON.toJSONString(userSetting), file);
                    } else {
                        String read = FileUtil.readUtf8String(file);
                        userSetting = JSON.parseObject(read, UserSetting.class);
                    }
                    // 使用默认设置
                    System.setProperty("webdriver.chrome.driver", setting.getDriver()); // 设置chromedriver
                }
            }
        }
        return userSetting;
    }

    public static void saveUserSetting() {
        UserSetting userSetting = getUserSetting();
        File file = new File(FILE_CONF_USER_SETTING);
        if (!file.exists()) {
            FileUtil.touch(file);
        }
        FileUtil.writeUtf8String(JSON.toJSONString(userSetting), file);
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
                        String submitScript = FileUtil.readUtf8String(domainPath + FILE_CONF_SCRIPTS_SUBMIT);

                        Assert.notBlank(parserScript, "parser.js script must be blank");
                        Assert.notBlank(executorScript, "executor.js script must be blank");
                        Assert.notBlank(submitScript, "submit.js script must be blank");

                        Script script = new Script();
                        script.setParserScript(parserScript);
                        script.setExecutorScript(executorScript);
                        script.setSubmitScript(submitScript);

                        scriptMap.put(domain, script);
                    }
                }
            }
        }
        return scriptMap;
    }
}
