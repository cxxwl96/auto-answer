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

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.func.Func0;
import cn.hutool.core.lang.func.VoidFunc0;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * CodePart
 *
 * @author cxxwl96
 * @since 2024/3/28 20:44
 */
@Slf4j
public class CodePart {

    /**
     * 延时，吞掉InterruptedException异常
     *
     * @param milliseconds 时间毫秒
     */
    public static void sleepNoneException(long milliseconds) {
        try {
            sleep(milliseconds);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * 延时
     *
     * @param milliseconds 时间毫秒
     */
    public static void sleep(long milliseconds) throws InterruptedException {
        if (milliseconds > 0) {
            Thread.sleep(milliseconds);
        }
    }

    /**
     * milliseconds毫秒之后执行
     *
     * @param milliseconds 时间毫秒
     * @param async        是否异步
     * @param callback     执行回调
     */
    public static void onTimeout(long milliseconds, boolean async, VoidFunc0 callback) {
        VoidFunc0 delayedCallback = () -> {
            Duration duration = Duration.ofMillis(milliseconds);
            LocalDateTime future = LocalDateTime.now().plusSeconds(duration.getSeconds());
            while (LocalDateTime.now().isBefore(future)) {
                sleepNoneException(10);
            }
            callback.call();
        };
        if (async) {
            new Thread(delayedCallback::callWithRuntimeException).start();
        } else {
            delayedCallback.callWithRuntimeException();
        }
    }

    /**
     * 每隔milliseconds毫秒执行一次
     *
     * @param milliseconds  时间毫秒
     * @param async         是否异步
     * @param callback      执行回调
     * @param stopCondition 停止执行条件
     */
    public static void onInterval(long milliseconds, boolean async, VoidFunc0 callback, Func0<Boolean> stopCondition) {
        VoidFunc0 delayedCallback = () -> {
            while (true) {
                callback.call();
                if (stopCondition.call()) {
                    break;
                }
                sleepNoneException(milliseconds);
            }
        };
        if (async) {
            new Thread(delayedCallback::callWithRuntimeException).start();
        } else {
            delayedCallback.callWithRuntimeException();
        }
    }

    /**
     * 重试
     *
     * @param times        重试次数
     * @param milliseconds 间隔时间：毫秒
     * @param callback     回调
     */
    public static void retryWithRuntimeException(int times, long milliseconds, VoidFunc0 callback) {
        try {
            retry(times, milliseconds, callback);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * 重试
     *
     * @param times        重试次数
     * @param milliseconds 间隔时间：毫秒
     * @param callback     回调
     * @throws Exception 调用异常
     */
    public static void retry(int times, long milliseconds, VoidFunc0 callback) throws Exception {
        boolean result = retry(times, milliseconds, () -> {
            callback.call();
            return true;
        });
    }

    /**
     * 重试
     *
     * @param times        重试次数
     * @param milliseconds 间隔时间：毫秒
     * @param callback     回调
     * @param <T>          结果类型
     * @return 结果
     */
    public static <T> T retryWithRuntimeException(int times, long milliseconds, Func0<T> callback) {
        try {
            return retry(times, milliseconds, callback);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * 重试
     *
     * @param times        重试次数
     * @param milliseconds 间隔时间：毫秒
     * @param callback     回调
     * @param <T>          结果类型
     * @return 结果
     * @throws Exception 调用异常
     */
    public static <T> T retry(int times, long milliseconds, Func0<T> callback) throws Exception {
        Assert.isTrue(times > 0, "times must be positive");
        Exception exception = null;
        for (int i = 0; i < times; i++) {
            try {
                return callback.call();
            } catch (Exception ex) {
                log.warn("Retry failed with count {}", i + 1, ex);
                exception = ex;
            }
            if (i < times - 1) {
                sleepNoneException(milliseconds);
            }
        }
        throw exception == null ? new RuntimeException("Failed to retry " + times + " times") : exception;
    }


    /**
     * 从范围内随机取一个数。
     *
     * @param start the start
     * @param end   the end
     * @return 数据范围内的随机数
     */
    public static int nextInRange(int start, int end) {
        int number = Math.max(start, end) - Math.min(start, end);
        return new Random().nextInt(number + 1) + Math.min(start, end);
    }
}
