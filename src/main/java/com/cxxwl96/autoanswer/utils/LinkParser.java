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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import lombok.Getter;

/**
 * lnk快捷方式解析
 *
 * @author cxxwl96
 * @since 2024/3/23 01:26
 */
@Getter
public class LinkParser {

    private boolean dir;

    private String realPath;

    public LinkParser(File f) throws IOException {
        parse(f);
    }

    private void parse(File f) throws IOException {
        // read the entire file into a byte buffer
        FileInputStream fin = new FileInputStream(f);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buff = new byte[256];
        while (true) {
            int n = fin.read(buff);
            if (n == -1) {
                break;
            }
            bout.write(buff, 0, n);
        }
        fin.close();
        byte[] link = bout.toByteArray();

        // get the flags byte
        byte flags = link[0x14];

        // get the file attributes byte
        final int file_atts_offset = 0x18;
        byte fileatts = link[file_atts_offset];
        byte is_dir_mask = (byte) 0x10;
        if ((fileatts & is_dir_mask) > 0) {
            dir = true;
        } else {
            dir = false;
        }

        // if the shell settings are present, skip them
        final int shell_offset = 0x4c;
        int shell_len = 0;
        if ((flags & 0x1) > 0) {
            // the plus 2 accounts for the length marker itself
            shell_len = bytes2short(link, shell_offset) + 2;
        }

        // get to the file settings
        int file_start = 0x4c + shell_len;

        // get the local volume and local system values
        int local_sys_off = link[file_start + 0x10] + file_start;
        realPath = getNullDelimitedString(link, local_sys_off);
    }

    static String getNullDelimitedString(byte[] bytes, int off) {
        int len = 0;
        // count bytes until the null character (0)
        while (true) {
            if (bytes[off + len] == 0) {
                break;
            }
            len++;
        }
        return new String(bytes, off, len);
    }

    // convert two bytes into a short // note, this is little endian because
    // it's for an // Intel only OS.
    static int bytes2short(byte[] bytes, int off) {
        return bytes[off] | (bytes[off + 1] << 8);
    }
}