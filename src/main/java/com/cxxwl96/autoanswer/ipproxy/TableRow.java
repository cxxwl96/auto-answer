/*
 * Copyright (c) 2021-2023, cxxwl96.com (cxxwl96@sina.com).
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

package com.cxxwl96.autoanswer.ipproxy;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;

/**
 * TableRow
 *
 * @author cxxwl96
 * @since 2023/11/26 18:49
 */
public class TableRow {
    private final SimpleStringProperty address;

    private final SimpleStringProperty proxyIP;

    private final SimpleStringProperty proxyLink;

    private final SimpleStringProperty createTime;

    private final SimpleStringProperty time;

    private TableCell<?, ?> tableCell;

    public TableRow(String address, String proxyIP, String proxyLink, String createTime, String time) {
        this.address = new SimpleStringProperty(address);
        this.proxyIP = new SimpleStringProperty(proxyIP);
        this.proxyLink = new SimpleStringProperty(proxyLink);
        this.createTime = new SimpleStringProperty(createTime);
        this.time = new SimpleStringProperty(time);
    }

    public String getAddress() {
        return address.get();
    }

    public SimpleStringProperty addressProperty() {
        return address;
    }

    public void setAddress(String address) {
        this.address.set(address);
    }

    public String getProxyIP() {
        return proxyIP.get();
    }

    public SimpleStringProperty proxyIPProperty() {
        return proxyIP;
    }

    public void setProxyIP(String proxyIP) {
        this.proxyIP.set(proxyIP);
    }

    public String getProxyLink() {
        return proxyLink.get();
    }

    public SimpleStringProperty proxyLinkProperty() {
        return proxyLink;
    }

    public void setProxyLink(String proxyLink) {
        this.proxyLink.set(proxyLink);
    }

    public String getCreateTime() {
        return createTime.get();
    }

    public SimpleStringProperty createTimeProperty() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime.set(createTime);
    }

    public String getTime() {
        return time.get();
    }

    public SimpleStringProperty timeProperty() {
        return time;
    }

    public void setTime(String time) {
        this.time.set(time);
    }

    public TableCell<?, ?> getTableCell() {
        return tableCell;
    }

    public void setTableCell(TableCell<?, ?> tableCell) {
        this.tableCell = tableCell;
    }
}
