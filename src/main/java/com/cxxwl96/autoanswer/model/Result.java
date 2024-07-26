/*
 * Copyright (c) 2021-2021, jad (cxxwl96@sina.com).
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

package com.cxxwl96.autoanswer.model;

import com.alibaba.fastjson.JSON;

import java.io.Serializable;

import lombok.Data;

/**
 * 响应结果
 *
 * @author cxxwl96
 * @since 2021/6/18 23:41
 */
@Data
public class Result<T> implements Serializable {
    public static final int CODE_SUCCESS = 200;

    public static final int CODE_FAILED = 400;

    private static final String DEFAULT_SUCCESS_MSG = "操作成功";

    private static final String DEFAULT_ERROR_MSG = "操作失败";

    private int code;

    private String msg;

    private T data;

    public boolean isSuccess() {
        return code == CODE_SUCCESS;
    }

    public String toJSONString() {
        return JSON.toJSONString(this);
    }

    public static ResultProcessor<?> with(boolean success) {
        final Result<?> result = success ? success() : failed();
        return new ResultProcessor<>(success, result);
    }

    public static Result<?> formatFailed(String format, Object... args) {
        return Result.failed(String.format(format, args));
    }

    public static Result<?> formatSuccess(String format, Object... args) {
        return Result.success(String.format(format, args));
    }

    public static <T> Result<T> success() {
        return success(CODE_SUCCESS, DEFAULT_SUCCESS_MSG, null);
    }

    public static <T> Result<T> success(String msg) {
        return success(CODE_SUCCESS, msg, null);
    }

    public static <T> Result<T> success(T data) {
        return success(CODE_SUCCESS, DEFAULT_SUCCESS_MSG, data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return success(CODE_SUCCESS, msg, data);
    }

    public static <T> Result<T> success(int code, String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> failed() {
        return failed(CODE_FAILED, DEFAULT_ERROR_MSG, null);
    }

    public static <T> Result<T> failed(String msg) {
        return failed(CODE_FAILED, msg, null);
    }

    public static <T> Result<T> failed(int code, String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    public static class ResultProcessor<T> {
        private final boolean success;

        private final Result<T> result;

        private ResultProcessor(boolean success, Result<T> result) {
            this.success = success;
            this.result = result;
        }

        public ResultProcessor<T> code(int code) {
            result.setCode(code);
            return this;
        }

        public ResultProcessor<T> msgSuccess(String msg) {
            if (success) {
                result.setMsg(msg);
            }
            return this;
        }

        public ResultProcessor<T> msgFailed(String msg) {
            if (!success) {
                result.setMsg(msg);
            }
            return this;
        }

        public ResultProcessor<T> data(T data) {
            result.setData(data);
            return this;
        }

        public Result<T> process() {
            return result;
        }
    }
}
