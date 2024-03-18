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
import com.cxxwl96.autoanswer.ipproxy.CityCode;
import com.cxxwl96.autoanswer.service.SurveyService;
import com.cxxwl96.autoanswer.service.impl.SurveyServiceImpl;
import com.cxxwl96.autoanswer.utils.Alert;
import com.cxxwl96.autoanswer.utils.TextAreaLog;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextFlow;
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
    private JFXTextField countText;

    @FXML
    private JFXRadioButton rangeProxy;

    @FXML
    private ToggleGroup proxy;

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
    private JFXButton submitBtn;

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

    @FXML
    private void onParse(ActionEvent event) {
        String domain = Optional.ofNullable(domainSelect.getSelectionModel())
            .map(SingleSelectionModel::getSelectedItem)
            .map(Label::getText)
            .orElse(null);
        if (StrUtil.isBlank(domain)) {
            Alert.error("请选择域");
            return;
        }
        String url = linkText.getText();
        if (StrUtil.isBlank(url)) {
            Alert.error("请输入链接");
            return;
        }
        if (!url.matches("^https?://.+$")) {
            Alert.error("无效的链接");
            return;
        }
        String id = surveyService.parseSurvey(url, domain);
        linkIdText.setText(id);
    }

    @FXML
    private void onRule(ActionEvent event) {
        String id = linkIdText.getText();
        if (StrUtil.isBlank(id)) {
            Alert.error("请输入链接ID");
            return;
        }
        String count = countText.getText();
        if (StrUtil.isBlank(count)) {
            Alert.error("请输入问卷数");
            return;
        }
        if (!NumberUtil.isInteger(count) || Integer.parseInt(count) <= 0) {
            Alert.error("问卷数必须是大于0的整数");
            return;
        }
        try {
            surveyService.editRule(id, Integer.parseInt(count));
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
        String id = linkIdText.getText();
        if (StrUtil.isBlank(id)) {
            Alert.error("请输入链接ID");
            return;
        }
        boolean isAssignProxy = assignProxy.isSelected();
        String province = provinceSelect.getSelectionModel().getSelectedItem().getText();
        String city = citySelect.getSelectionModel().getSelectedItem().getText();
        try {
            surveyService.submit(id, isAssignProxy, province, city);
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }
    }
}

