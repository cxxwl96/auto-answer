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

import com.cxxwl96.autoanswer.context.AutoAnswerContext;
import com.cxxwl96.autoanswer.enums.ProxyType;
import com.cxxwl96.autoanswer.enums.UserAgentType;
import com.cxxwl96.autoanswer.ipproxy.CityCode;
import com.cxxwl96.autoanswer.model.SubmitInfo;
import com.cxxwl96.autoanswer.service.SurveyService;
import com.cxxwl96.autoanswer.service.impl.SurveyServiceImpl;
import com.cxxwl96.autoanswer.utils.Alert;
import com.cxxwl96.autoanswer.utils.ApplicationStore;
import com.cxxwl96.autoanswer.utils.Confirm;
import com.cxxwl96.autoanswer.utils.FXMLUtil;
import com.cxxwl96.autoanswer.utils.SeleniumUtil;
import com.cxxwl96.autoanswer.utils.TextAreaLog;
import com.cxxwl96.updater.client.UpdaterClient;
import com.cxxwl96.updater.client.model.PropertyKeys;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndexController implements Initializable {
    @FXML
    private JFXComboBox<Label> domainSelect;

    @FXML
    private JFXTextField linkText;

    @FXML
    private JFXButton parseBtn;

    @FXML
    private JFXTextField linkIdText;

    @FXML
    private JFXButton ruleBtn;

    @FXML
    private ToggleGroup proxyTG;

    @FXML
    private JFXRadioButton assignProxy;

    @FXML
    private HBox provinceHBox;

    @FXML
    private HBox cityHBox;

    @FXML
    private JFXComboBox<Label> provinceSelect;

    @FXML
    private JFXComboBox<Label> citySelect;

    @FXML
    private ToggleGroup userAgentTG;

    @FXML
    private JFXCheckBox autoSubmit;

    @FXML
    private JFXCheckBox autoClose;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private TextFlow textFlow;

    private final SurveyService surveyService = new SurveyServiceImpl();

    private final TextAreaLog logger = TextAreaLog.getLogger(log);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initDomainSelect();
        initProxyAddress();
        initLogger();
    }

    private void initLogger() {
        TextAreaLog.TEXT_FLOW = textFlow;

        // 日志内容变化时设置滚动条滚动到底部
        textFlow.getChildren().addListener((ListChangeListener<Node>) c -> {
            // 获取内容节点
            double maxHeight = scrollPane.getContent().getBoundsInParent().getMaxY();
            // 获取ScrollPane的可视区域的最大y坐标
            double vValue = maxHeight - scrollPane.getHeight();
            // 如果内容高度大于ScrollPane的高度，则滚动到底部
            if (vValue > 0) {
                scrollPane.setVvalue(1.0);
            }
        });

        // 添加右键清除按钮
        final MenuItem menuItem = new MenuItem("清除日志");
        menuItem.setOnAction(event -> textFlow.getChildren().clear());
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().add(menuItem);
        scrollPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(scrollPane, event.getScreenX(), event.getScreenY());
            }
        });
    }

    private void initProxyAddress() {
        // 可见性绑定
        provinceHBox.visibleProperty().bindBidirectional(assignProxy.selectedProperty());
        cityHBox.visibleProperty().bindBidirectional(assignProxy.selectedProperty());
        // 初始化省
        ObservableList<Label> provinceList = FXCollections.observableArrayList(
            CityCode.getProvinces().stream().map(Label::new).collect(Collectors.toList()));
        provinceSelect.setItems(provinceList);
        provinceSelect.getSelectionModel().selectFirst();
        // 初始化市
        String provinceName = provinceSelect.getSelectionModel().getSelectedItem().getText();
        ObservableList<Label> cityList = FXCollections.observableArrayList(
            CityCode.getCities(provinceName).stream().map(Label::new).collect(Collectors.toList()));
        citySelect.setItems(cityList);
        citySelect.getSelectionModel().selectFirst();
    }

    private void initDomainSelect() {
        Set<String> domains = AutoAnswerContext.getScriptMap().keySet();
        List<Label> domainList = domains.stream().map(domain -> {
            Label label = new Label();
            label.setText(domain);
            return label;
        }).collect(Collectors.toList());
        domainSelect.setItems(FXCollections.observableArrayList(domainList));
        domainSelect.getSelectionModel().selectFirst();
    }

    @SneakyThrows
    @FXML
    private void onSetting(ActionEvent event) {
        Scene scene = new Scene(FXMLUtil.load("/views/Setting.fxml"));
        scene.getStylesheets().add("/assets/css/app-light.css");

        Stage window = ApplicationStore.get("window", Stage.class);

        Stage settingStage = new Stage();
        settingStage.setTitle("设置");
        settingStage.getIcons().add(ApplicationStore.get("logo", Image.class));
        settingStage.initOwner(window);
        settingStage.initModality(Modality.APPLICATION_MODAL);
        settingStage.setScene(scene);
        settingStage.setOnCloseRequest(Event::consume);
        settingStage.initStyle(StageStyle.UTILITY);
        settingStage.show();
    }

    @FXML
    private void onUpdating(ActionEvent event) {
        Properties props = SystemUtil.getProps();
        props.setProperty(PropertyKeys.HOST,
            Optional.ofNullable(AutoAnswerContext.getSettingValue(PropertyKeys.HOST)).orElse(StrUtil.EMPTY));
        props.setProperty(PropertyKeys.APP_NAME,
            Optional.ofNullable(AutoAnswerContext.getSettingValue(PropertyKeys.APP_NAME)).orElse(StrUtil.EMPTY));
        props.setProperty(PropertyKeys.APP_VERSION,
            Optional.ofNullable(AutoAnswerContext.getSettingValue(PropertyKeys.APP_VERSION)).orElse(StrUtil.EMPTY));
        try {
            Window window = ((Node) event.getSource()).getScene().getWindow();
            new UpdaterClient().start(((Stage) window));
        } catch (Exception exception) {
            Alert.error(exception.getMessage());
        }
    }

    @FXML
    private void onParse(ActionEvent event) {
        String domain = getDomain();
        String url = linkText.getText();
        try {
            String id = surveyService.parseSurvey(domain, url);
            linkIdText.setText(id);
        } catch (Exception exception) {
            Alert.error(exception.getMessage());
        }
    }

    @FXML
    private void onRule(ActionEvent event) {
        String id = linkIdText.getText();
        try {
            surveyService.editRule(id);
        } catch (Exception exception) {
            Alert.error(exception.getMessage());
        }
    }

    @FXML
    private void provinceChange(ActionEvent event) {
        // 初始化市
        String provinceName = provinceSelect.getSelectionModel().getSelectedItem().getText();
        ObservableList<Label> cityList = FXCollections.observableArrayList(
            CityCode.getCities(provinceName).stream().map(Label::new).collect(Collectors.toList()));
        citySelect.setItems(cityList);
        citySelect.getSelectionModel().selectFirst();
    }

    @FXML
    private void onSubmit() {
        String domain = getDomain();
        String id = linkIdText.getText();
        String proxyName = proxyTG.getSelectedToggle().getUserData().toString();
        ProxyType proxyType = ProxyType.valueOf(proxyName);
        String province = provinceSelect.getSelectionModel().getSelectedItem().getText();
        String city = citySelect.getSelectionModel().getSelectedItem().getText();
        String userAgentName = userAgentTG.getSelectedToggle().getUserData().toString();
        UserAgentType userAgentType = UserAgentType.valueOf(userAgentName);
        boolean isAutoSubmit = autoSubmit.isSelected();
        boolean isAutoClose = autoClose.isSelected();

        SubmitInfo submitInfo = new SubmitInfo();
        submitInfo.setProxyType(proxyType);
        submitInfo.setProvince(province);
        submitInfo.setCity(city);
        submitInfo.setUserAgentType(userAgentType);
        submitInfo.setAutoSubmit(isAutoSubmit);
        submitInfo.setAutoClose(isAutoClose);

        try {
            surveyService.submit(domain, id, submitInfo);
        } catch (Exception exception) {
            Alert.error(exception.getMessage());
        }
    }

    @FXML
    private void onStop() {
        try {
            surveyService.stop();
        } catch (Exception exception) {
            Alert.error(exception.getMessage());
        }
    }

    @FXML
    private void onCloseAllBrowser(ActionEvent event) {
        Confirm.buildConfirm().okAction(e -> {
            logger.warn("关闭所有浏览器");
            new Thread(SeleniumUtil::quitAll).start();
        }).warn("正在答题的浏览器也将被关闭，确认关闭？");
    }

    private String getDomain() {
        return Optional.ofNullable(domainSelect.getSelectionModel())
            .map(SingleSelectionModel::getSelectedItem)
            .map(Label::getText)
            .orElse(null);
    }
}
