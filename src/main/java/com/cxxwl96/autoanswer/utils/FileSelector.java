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

import java.io.File;
import java.nio.file.StandardCopyOption;
import java.util.List;

import cn.hutool.core.io.FileUtil;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * FileSelector
 *
 * @author cxxwl96
 * @since 2024/07/27 22:45
 */
public class FileSelector {
    /**
     * 选择文件夹
     *
     * @return 文件夹
     */
    public static File chooseDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择文件夹");
        return directoryChooser.showDialog(ApplicationStore.get("window", Stage.class));
    }

    /**
     * 选择单文件
     *
     * @param extensionFilters 文件类型过滤
     * @return 文件
     */
    public static File chooseFile(FileChooser.ExtensionFilter... extensionFilters) {
        return buildFileChooser(extensionFilters).showOpenDialog(ApplicationStore.get("window", Stage.class));
    }

    /**
     * 选择单文件
     *
     * @param extensionFilters 文件类型过滤
     * @return 文件
     */
    public static List<File> chooseFiles(FileChooser.ExtensionFilter... extensionFilters) {
        return buildFileChooser(extensionFilters).showOpenMultipleDialog(ApplicationStore.get("window", Stage.class));
    }

    /**
     * extensionFilters
     * <li>new FileChooser.ExtensionFilter("Text Files", "*.txt");</li>
     * <li>new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif");</li>
     * <li>new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac");</li>
     * <li>new FileChooser.ExtensionFilter("All Files", "*.*");</li>
     *
     * @param extensionFilters 文件类型过滤
     * @return FileChooser
     */
    private static FileChooser buildFileChooser(FileChooser.ExtensionFilter... extensionFilters) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择文件");
        fileChooser.getExtensionFilters().addAll(extensionFilters);
        return fileChooser;
    }

    /**
     * 保存文件
     *
     * @param file 文件
     */
    public static void saveFile(File file) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存文件");
        fileChooser.setInitialFileName(file.getName());
        File targetFile = fileChooser.showSaveDialog(ApplicationStore.get("window", Stage.class));
        if (targetFile != null) {
            FileUtil.copyFile(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
