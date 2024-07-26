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

import com.cxxwl96.autoanswer.model.SubjectAnswer;

import java.util.List;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * ExpressionUtil
 *
 * @author cxxwl96
 * @since 2024/3/9 22:36
 */
@Slf4j
public class ExpressionUtil {

    private static final String TRUE = "true";

    private static final String FALSE = "false";

    private static final String AND = "&&";

    private static final String OR = "||";

    private static final String NOT = "!";

    private static final String EQUALS = "=";

    private static final String NOT_EQUALS = "!=";

    /**
     * 表达式解析，例如：
     *
     * @param expression 表达式，例如：(!(1=A || 1=B && 2=C) && (10!= C || 2!=B)) || 3=E
     * @param subjectAnswers 问卷答案
     * @return
     */
    public static boolean parseExpression(String expression, List<SubjectAnswer> subjectAnswers) {
        String transferExp = null;
        try {
            transferExp = transferExpression(expression, subjectAnswers);
            String result = dealParseExpression(transferExp);
            log.info("[parse expression] expression: {}, transfer expression: {}, result: {}", expression, transferExp, result);
            return TRUE.equals(result);
        } catch (StackOverflowError error) {
            log.error("expression: {}, transfer expression: {}", expression, transferExp, error);
            throw new RuntimeException("不合法的表达式: " + expression + ", 请检查括号是否匹配");
        } catch (Exception exception) {
            throw new RuntimeException("不合法的表达式: " + exception.getMessage());
        }
    }

    private static String transferExpression(String expression, List<SubjectAnswer> subjectAnswers) {
        // 未填写规则默认可选
        if (StrUtil.isBlank(expression)) {
            return TRUE;
        }
        // 去掉空白字符
        expression = expression.replaceAll("[\\n\\t\\r\\f\\v\\s]", "");

        char[] chars = expression.toCharArray();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < chars.length; ) {
            char ch = chars[i];
            if (ch == '(' || ch == ')') {
                sb.append(ch);
                i++;
            } else if ('0' <= ch && ch <= '9') {
                // 找到题目编号
                int start = i;
                i++;
                while ('0' <= chars[i] && chars[i] <= '9') {
                    i++;
                }
                String noStr = subString(expression, start, i);
                int no = Integer.parseInt(noStr); // 题目编号
                // 找到条件
                String condition;
                if (EQUALS.equals(subString(expression, i, i + EQUALS.length()))) {
                    condition = EQUALS;
                    i += condition.length();
                } else if (NOT_EQUALS.equals(subString(expression, i, i + NOT_EQUALS.length()))) {
                    condition = NOT_EQUALS;
                    i += condition.length();
                } else {
                    throw new RuntimeException(subString(expression, 0, i));
                }
                // 找到选项
                String option = subString(expression, i, i + 1);
                if (!option.matches("[a-zA-Z]")) {
                    throw new RuntimeException(
                        subString(expression, 0, i + 1) + ", 表达式中题目选项不正确, 不应写" + no + condition + option);
                }
                option = option.toUpperCase();
                // 计算 no+condition+option 是否为真
                RuntimeException exception = new RuntimeException(subString(expression, 0, i + 1) + ", 表达式中题目序号不正确");
                if (no >= subjectAnswers.size()) {
                    throw exception;
                }
                // 找到对应题目
                SubjectAnswer subjectAnswer = subjectAnswers.stream()
                    .filter(sa -> sa.getNo() == no)
                    .findFirst()
                    .orElseThrow(() -> exception);
                // 题目是否选择
                boolean result = subjectAnswer.getAnswers().contains(option);
                result = EQUALS.equals(condition) == result;
                sb.append(result ? TRUE : FALSE);
                i++;
            } else if (OR.equals(subString(expression, i, i + OR.length()))) {
                sb.append(OR);
                i += OR.length();
            } else if (AND.equals(subString(expression, i, i + AND.length()))) {
                sb.append(AND);
                i += AND.length();
            } else if (NOT.equals(subString(expression, i, i + NOT.length()))) {
                sb.append(NOT);
                i += NOT.length();
            } else {
                throw new RuntimeException(subString(expression, 0, i + 1));
            }
        }
        return sb.toString();
    }

    private static String dealParseExpression(String expression) {
        if (TRUE.equals(expression)) {
            return TRUE;
        }
        if (FALSE.equals(expression)) {
            return FALSE;
        }
        // "t|t"
        expression = expression.replace(TRUE + OR + TRUE, TRUE);
        // "t|f"
        expression = expression.replace(TRUE + OR + FALSE, TRUE);
        // "f|t"
        expression = expression.replace(FALSE + OR + TRUE, TRUE);
        // "f|f"
        expression = expression.replace(FALSE + OR + FALSE, FALSE);
        // "t&t"
        expression = expression.replace(TRUE + AND + TRUE, TRUE);
        // "t&f"
        expression = expression.replace(TRUE + AND + FALSE, FALSE);
        // "f&t"
        expression = expression.replace(FALSE + AND + TRUE, FALSE);
        // "f&f"
        expression = expression.replace(FALSE + AND + FALSE, FALSE);
        // "(t)"
        expression = expression.replace("(" + TRUE + ")", TRUE);
        // "(f)"
        expression = expression.replace("(" + FALSE + ")", FALSE);
        // "!t"
        expression = expression.replace(NOT + TRUE, FALSE);
        // "!f"
        expression = expression.replace(NOT + FALSE, TRUE);
        return dealParseExpression(expression);
    }

    private static String subString(String expression, int start, int end) {
        return expression.substring(start, Math.min(end, expression.length()));
    }

}
