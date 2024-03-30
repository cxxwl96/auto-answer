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
package com.cxxwl96.autoanswer;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpUtil;

import java.io.File;
import java.io.IOException;

/**
 * Test
 *
 * @author cxxwl96
 * @since 2024/3/29 20:39
 */
public class Test {
//    private static final WebDriver driver;
//
//    static {
//        System.setProperty("webdriver.chrome.driver", "bin/chrome/mac/chromedriver"); // 设置chromedriver
//        driver = SeleniumUtil.newChromeDriver(SeleniumUtil.newChromeOptions());
//        driver.get("chrome://version");
//        CodePart.sleepNoneException(100000);
//        driver.quit();
//    }

    public static void main(String[] args) throws IOException {
        StreamProgress streamProgress = new StreamProgress() {
            @Override
            public void start() {
                System.out.println("开始下载");
            }

            @Override
            public void progress(long total, long progressSize) {
                System.out.printf("%.2f/%.2f\n", progressSize * 1.0 / 1024 / 1024, total * 1.0 / 1024 / 1024);
            }

            @Override
            public void finish() {
                System.out.println("下载完成");
            }
        };
        String url = "https://dldir1.qq.com/qqfile/qq/QQNT/Mac/QQ_6.9.30_240322_01.dmg";
        File targetFile = new File("./QQ_6.9.30_240322_01.dmg");

//        long size = HttpUtil.download(url, Files.newOutputStream(targetFile.toPath()), true, streamProgress);

        long size = HttpUtil.downloadFile(url, targetFile, streamProgress);
        System.out.println(size);
    }
}
