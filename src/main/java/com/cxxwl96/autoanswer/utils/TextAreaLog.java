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

import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.application.Platform;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.Getter;

/**
 * TextAreaLog
 *
 * @author cxxwl96
 * @since 2024/3/9 00:41
 */
public class TextAreaLog {
    private final Logger log;

    public static TextFlow TEXT_FLOW = null;

    private enum LogLevel {
        INFO("INFO", "#000000"),
        ERROR("ERROR", "#ff0000"),
        WARN("WARN", "#ff8800"),
        SUCCESS("SUCCESS", "#00CC00"),
        ;

        @Getter
        private final String level;

        @Getter
        private final String color;

        LogLevel(String level, String color) {
            this.level = level;
            this.color = color;
        }
    }

    private TextAreaLog(Logger log) {
        this.log = log;
    }

    public static TextAreaLog getLogger(Logger logger) {
        return new TextAreaLog(logger);
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    public void success(String message) {
        log(LogLevel.SUCCESS, message);
    }

    private void log(LogLevel level, String message) {
        switch (level) {
            case INFO:
            case SUCCESS:
                log.info(message);
                break;
            case WARN:
                log.warn(message);
                break;
            case ERROR:
                log.error(message);
                break;
        }

        Platform.runLater(() -> {
            if (TEXT_FLOW == null) {
                return;
            }
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String format = String.format("%s [%s] > %s\n", now, level.getLevel(), message);
            int buffer = AutoAnswerContext.getSetting().getLoggerBuffer();
            if (TEXT_FLOW.getChildren().size() > buffer) {
                TEXT_FLOW.getChildren().clear();
            }
            Text text = new Text();
            text.setStyle("-fx-fill: " + level.getColor() + ";");
            text.setText(format);
            TEXT_FLOW.getChildren().add(text);
        });
    }
}
