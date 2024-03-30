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

package com.cxxwl96.autoanswer.views;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.system.SystemUtil;
import com.cxxwl96.autoanswer.context.AutoAnswerContext;
import com.cxxwl96.autoanswer.context.UserSetting;
import com.cxxwl96.autoanswer.utils.Alert;
import com.cxxwl96.autoanswer.utils.LinkParser;
import com.cxxwl96.autoanswer.utils.TextAreaLog;
import com.cxxwl96.autoanswer.views.component.JFXNumberField;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * SettingController
 *
 * @author cxxwl96
 * @since 2024/3/22 23:02
 */
@Slf4j
public class SettingController implements Initializable {
    @FXML
    private JFXTextField chromeAddressText;

    @FXML
    private JFXNumberField chromeCountText;

    @FXML
    private JFXNumberField segmentStartText;

    @FXML
    private JFXNumberField segmentEndText;

    @FXML
    private JFXNumberField submitStartText;

    @FXML
    private JFXNumberField submitEndText;

    @FXML
    private JFXNumberField pageLoadTimeoutText;

    @FXML
    private JFXTextField homeUrlText;

    @FXML
    private JFXTextField ipUrlText;

    private final TextAreaLog logger = TextAreaLog.getLogger(log);

    private UserSetting userSetting;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userSetting = AutoAnswerContext.getUserSetting();
        chromeAddressText.textProperty().bindBidirectional(userSetting.chromeAddressProperty());
        chromeCountText.textProperty().bindBidirectional(userSetting.chromeCountProperty());
        segmentStartText.textProperty().bindBidirectional(userSetting.segmentStartProperty());
        segmentEndText.textProperty().bindBidirectional(userSetting.segmentEndProperty());
        submitStartText.textProperty().bindBidirectional(userSetting.submitStartProperty());
        submitEndText.textProperty().bindBidirectional(userSetting.submitEndProperty());
        pageLoadTimeoutText.textProperty().bindBidirectional(userSetting.pageLoadTimeoutProperty());
        homeUrlText.textProperty().bindBidirectional(userSetting.homeUrlProperty());
        ipUrlText.textProperty().bindBidirectional(userSetting.ipUrlProperty());
    }

    @FXML
    void onDragOver(DragEvent event) {
        // 当文件拖入时，允许复制操作
        if (event.getGestureSource() != chromeAddressText && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    @SneakyThrows
    @FXML
    void onDragDropped(DragEvent event) {
        // 获取拖入的文件列表
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            String path = db.getFiles().get(0).getPath();
            log.info("On drag dropped path: " + path);
            if (path.endsWith(".lnk")) {
                LinkParser parser = new LinkParser(new File(path));
                path = parser.getRealPath();
            }
            log.info("Real path: " + path);
            chromeAddressText.setText(path);
        }
        event.consume();
    }

    @FXML
    public void onDefaultBrowser(ActionEvent event) {
        String binary = AutoAnswerContext.getSetting().getBinary();
        chromeAddressText.setText(binary);

        AutoAnswerContext.saveUserSetting();
    }

    @FXML
    void onSave(ActionEvent event) {
        try {
            Assert.notBlank(userSetting.getChromeAddress(), "请设置Google浏览器地址");
            if (SystemUtil.getOsInfo().isWindows()) {
                Assert.isTrue(userSetting.getChromeAddress().endsWith(".exe"), "浏览器地址不正确");
            }
            Assert.isTrue(NumberUtil.parseInt(userSetting.getChromeCount()) > 0, "同时最多打开浏览器个数必须大于0");
            Assert.isTrue(NumberUtil.parseInt(userSetting.getSegmentStart()) > 0, "问卷间隔时间范围必须大于0");
            Assert.isTrue(NumberUtil.parseInt(userSetting.getSegmentEnd()) > 0, "问卷间隔时间范围必须大于0");
            Assert.isTrue(NumberUtil.parseInt(userSetting.getSubmitStart()) > 0, "问卷提交时间范围必须大于0");
            Assert.isTrue(NumberUtil.parseInt(userSetting.getSubmitEnd()) > 0, "问卷提交时间范围必须大于0");
            Assert.isTrue(NumberUtil.parseInt(userSetting.getPageLoadTimeout()) > 0, "打开问卷超时时间必须大于0");

            AutoAnswerContext.saveUserSetting();

            JFXButton closeBtn = (JFXButton) event.getSource();
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();
        } catch (Exception exception) {
            Alert.error(exception.getMessage());
        }
    }

}
