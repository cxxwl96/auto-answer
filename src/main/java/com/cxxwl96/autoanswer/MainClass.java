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

import com.cxxwl96.autoanswer.utils.ApplicationStore;
import com.cxxwl96.autoanswer.utils.FXMLUtil;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * MainClass
 *
 * @author cxxwl96
 * @since 2024/3/1 22:17
 */
@Slf4j
public class MainClass extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        ApplicationStore.register("window", stage);
        Image logo = new Image(FXMLUtil.resource("/logo.png"));
        ApplicationStore.register("logo", logo);
        stage.setTitle("答题助手");
        stage.getIcons().add(logo);
        stage.setOnCloseRequest((event) -> System.exit(0));
        Scene scene = new Scene(FXMLUtil.load("/views/Index.fxml"));
        scene.getStylesheets().add("/assets/css/app-light.css");
        stage.setScene(scene);
        stage.show();
    }
}
