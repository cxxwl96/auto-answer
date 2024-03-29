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

package com.cxxwl96.autoanswer.utils;

import java.io.IOException;
import java.io.InputStream;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

/**
 * FXMLUtil
 *
 * @author cxxwl96
 * @since 2023/11/26 18:06
 */
public class FXMLUtil {
    public static <T extends Node> T load(String path) throws IOException {
        final FXMLLoader loader = new FXMLLoader();
        try {
            return loader.load(resource(path));
        } catch (IOException exception) {
            throw new IOException("Failed to load " + path, exception);
        }
    }

    public static InputStream resource(String path) {
        return FXMLUtil.class.getResourceAsStream(path);
    }
}
