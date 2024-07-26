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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * UserSetting
 *
 * @author cxxwl96
 * @since 2024/3/23 18:48
 */
public class UserSetting {
    // 设置版本
    @Getter
    @Setter
    private String version = "1.0.0";

    // Google浏览器地址
    private StringProperty chromeAddress = new SimpleStringProperty("");

    // 同时最多打开浏览器个数
    private StringProperty chromeCount = new SimpleStringProperty("4");

    // 问卷间隔时间范围（单位：s）
    private StringProperty segmentStart = new SimpleStringProperty("10");

    private StringProperty segmentEnd = new SimpleStringProperty("20");

    // 问卷提交时间范围（单位：s）
    private StringProperty submitStart = new SimpleStringProperty("60");

    private StringProperty submitEnd = new SimpleStringProperty("120");

    // 打开问卷超时时间（单位：s）
    private StringProperty pageLoadTimeout = new SimpleStringProperty("20");

    // 浏览器首页
    private StringProperty homeUrl = new SimpleStringProperty("https://www.baidu.com");

    // 提交问卷时附带打开的IP查看网址
    private StringProperty ipUrl = new SimpleStringProperty("https://www.ip138.com");

    public String getChromeAddress() {
        return chromeAddress.get();
    }

    public StringProperty chromeAddressProperty() {
        return chromeAddress;
    }

    public void setChromeAddress(String chromeAddress) {
        this.chromeAddress.set(chromeAddress);
    }

    public String getChromeCount() {
        return chromeCount.get();
    }

    public StringProperty chromeCountProperty() {
        return chromeCount;
    }

    public void setChromeCount(String chromeCount) {
        this.chromeCount.set(chromeCount);
    }

    public String getSegmentStart() {
        return segmentStart.get();
    }

    public StringProperty segmentStartProperty() {
        return segmentStart;
    }

    public void setSegmentStart(String segmentStart) {
        this.segmentStart.set(segmentStart);
    }

    public String getSegmentEnd() {
        return segmentEnd.get();
    }

    public StringProperty segmentEndProperty() {
        return segmentEnd;
    }

    public void setSegmentEnd(String segmentEnd) {
        this.segmentEnd.set(segmentEnd);
    }

    public String getSubmitStart() {
        return submitStart.get();
    }

    public StringProperty submitStartProperty() {
        return submitStart;
    }

    public void setSubmitStart(String submitStart) {
        this.submitStart.set(submitStart);
    }

    public String getSubmitEnd() {
        return submitEnd.get();
    }

    public StringProperty submitEndProperty() {
        return submitEnd;
    }

    public void setSubmitEnd(String submitEnd) {
        this.submitEnd.set(submitEnd);
    }

    public String getPageLoadTimeout() {
        return pageLoadTimeout.get();
    }

    public StringProperty pageLoadTimeoutProperty() {
        return pageLoadTimeout;
    }

    public void setPageLoadTimeout(String pageLoadTimeout) {
        this.pageLoadTimeout.set(pageLoadTimeout);
    }

    public String getHomeUrl() {
        return homeUrl.get();
    }

    public StringProperty homeUrlProperty() {
        return homeUrl;
    }

    public void setHomeUrl(String homeUrl) {
        this.homeUrl.set(homeUrl);
    }

    public String getIpUrl() {
        return ipUrl.get();
    }

    public StringProperty ipUrlProperty() {
        return ipUrl;
    }

    public void setIpUrl(String ipUrl) {
        this.ipUrl.set(ipUrl);
    }
}
