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

package com.cxxwl96.autoanswer.utils;

import com.cxxwl96.autoanswer.context.AutoAnswerContext;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * SeleniumUtil
 *
 * @author cxxwl96
 * @since 2024/3/20 21:59
 */
@Slf4j
public class SeleniumUtil {
    private static final Set<WebDriver> WEB_DRIVERS = new HashSet<>();

    /**
     * 新建Chrome驱动
     *
     * @param args 启动参数
     * @return web driver
     */
    public static WebDriver newChromeDriver(String... args) {
        return newChromeDriver(newChromeOptions(args));
    }

    public static WebDriver newChromeDriver(ChromeOptions chromeOptions) {
        WebDriver driver = new ChromeDriver(chromeOptions);
        WEB_DRIVERS.add(driver);
        return driver;
    }

    /**
     * 关掉driver
     *
     * @param webDriver web driver
     */
    public static void quit(WebDriver webDriver) {
        try {
            if (webDriver != null) {
                webDriver.quit();
            }
        } finally {
            WEB_DRIVERS.remove(webDriver);
        }
    }

    /**
     * 关掉所有driver
     */
    public static void quitAll() {
        // TODO 定时清空 WEB_DRIVERS.clear();
        for (WebDriver webDriver : WEB_DRIVERS) {
            try {
                webDriver.quit();
            } catch (Exception ignored) {
            }
        }
    }

    public static ChromeOptions newChromeOptions(String... args) {
        String webBinary = AutoAnswerContext.getUserSetting().getChromeAddress();

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setBinary(new File(webBinary));

        chromeOptions.addArguments("--lang=zh-CN"); // 设置中文简体
        chromeOptions.addArguments("--allow-running-insecure-content"); // 消除安全校验
        chromeOptions.addArguments("--window-size=830,630"); // 设置浏览器打开窗口的大小,非必要属性
        // chromeOptions.addArguments("--headless"); // 无头模式，不显示浏览器窗口
        // chromeOptions.addArguments("--start-maximized"); // 启动最大化
        // chromeOptions.addArguments("--auto-open-devtools-for-tabs"); // 打开控制台
        if (args != null) {
            chromeOptions.addArguments(args);
        }
        return chromeOptions;
    }
}
