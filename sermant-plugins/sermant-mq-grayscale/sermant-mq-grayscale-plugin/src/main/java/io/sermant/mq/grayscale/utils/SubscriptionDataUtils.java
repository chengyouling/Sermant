/*
 * Copyright (C) 2024-2024 Sermant Authors. All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.sermant.mq.grayscale.utils;

import io.sermant.core.common.LoggerFactory;
import io.sermant.core.utils.StringUtils;
import io.sermant.mq.grayscale.config.GrayTagItem;
import io.sermant.mq.grayscale.config.MqGrayscaleConfig;

import org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * TAG/SQL92 query message statement builder util
 *
 * @author chengyouling
 * @since 2024-06-03
 */
public class SubscriptionDataUtils {
    public static final String EXPRESSION_TYPE_TAG = "TAG";

    public static final String EXPRESSION_TYPE_SQL92 = "SQL92";

    private static final Pattern pattern = Pattern.compile("and|or", Pattern.CASE_INSENSITIVE);

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final String CONSUME_TYPE_ALL = "all";

    private static final String CONSUME_TYPE_BASE = "base";

    private final static Map<String, List<GrayTagItem>> AUTO_CHECK_GRAY_TAGS = new ConcurrentHashMap<>();

    private final static String AUTO_CHECK_GRAY_TAG_KEY = "autoCheckGrayTags";

    private SubscriptionDataUtils() {}

    public static String buildSQL92ExpressionByTags(Set<String> tagsSet) {
        return tagsSet != null && !tagsSet.isEmpty() ? buildTagsExpression(tagsSet) : " ";
    }

    private static String buildTagsExpression(Set<String> tagsSet) {
        return  " ( TAGS is not null and TAGS in " + getStrForSets(tagsSet) + " ) ";
    }

    private static String getStrForSets(Set<String> tags) {
        StringBuilder builder = new StringBuilder("(");
        for (String tag : tags) {
            builder.append("'").append(tag).append("'");
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(") ");
        return builder.toString();
    }

    public static String addMseGrayTagsToSQL92Expression(String originSubData) {
        if (!StringUtils.isBlank(originSubData)) {
            originSubData = removeMseGrayTagFromOriginSubData(originSubData);
        }
        String sql92Expression = buildSQL92Expression();
        if (StringUtils.isBlank(sql92Expression)) {
            return originSubData;
        } else {
            return StringUtils.isBlank(originSubData) ? sql92Expression : originSubData + " and " + sql92Expression;
        }
    }

    private static String buildSQL92Expression() {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isEmpty(MqGrayscaleConfigUtils.getGrayGroupTag())) {
            // all模式所有消息返回
            if (CONSUME_TYPE_ALL.equals(MqGrayscaleConfigUtils.getConsumeType())) {
                return "";
            }
            // base模式只返回不带标签消息
            if (CONSUME_TYPE_BASE.equals(MqGrayscaleConfigUtils.getConsumeType())) {
                MqGrayscaleConfig config = MqGrayscaleConfigUtils.getGrayscaleConfigs();
                if (config != null && !config.getGrayscale().isEmpty()) {
                    sb.append(" ( ");
                    sb.append(buildBaseTypeTagSql(config.getGrayscale()));
                    sb.append(" ) ");
                }
                return sb.toString();
            }
            if (AUTO_CHECK_GRAY_TAGS.get(AUTO_CHECK_GRAY_TAG_KEY) == null) {
                return "";
            }
            sb.append(" ( ")
                .append(buildAutoTypeTagSql(AUTO_CHECK_GRAY_TAGS.get(AUTO_CHECK_GRAY_TAG_KEY)))
                .append(" ) ");
        } else {
            List<GrayTagItem> items = MqGrayscaleConfigUtils.getGrayscaleConfigs().getGrayscale();
            GrayTagItem grayTagItem
                = MqGrayscaleConfigUtils.getScaleByGroupTag(MqGrayscaleConfigUtils.getGrayGroupTag(), items);
            if (grayTagItem != null) {
                sb.append(" ( ")
                    .append(buildGrayscaleTagSql(grayTagItem))
                    .append(" ) ");
            }
        }
        return sb.toString();
    }

    private static String buildGrayscaleTagSql(GrayTagItem item) {
        Map<String, List<String>> tagMap = new HashMap<>();
        for (Map.Entry<String, String> envEntry : item.getEnvTag().entrySet()) {
            resetMapInfo(tagMap, envEntry);
        }
        for (Map.Entry<String, String> entry : item.getTrafficTag().entrySet()) {
            resetMapInfo(tagMap, entry);
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<String>> envEntry : tagMap.entrySet()) {
            if (builder.length() > 0) {
                builder.append(" or ");
            }
            builder.append("( ")
                .append(envEntry.getKey())
                .append(" in ")
                .append(getStrForSets(new HashSet<>(envEntry.getValue())))
                .append(")");
        }
        return builder.length() > 0 ? builder.toString() : "";
    }

    private static String buildBaseTypeTagSql(List<GrayTagItem> items) {
        StringBuilder builder = new StringBuilder();
        Set<String> set = MqGrayscaleConfigUtils.buildGrayTagKeySet(items);
        for (String env : set) {
            if (builder.length() > 0) {
                builder.append(" and ");
            }
            builder.append("( ")
                .append(env)
                .append(" is null ")
                .append(")");
        }
        return builder.length() > 0 ? builder.toString() : "";
    }

    private static String buildAutoTypeTagSql(List<GrayTagItem> items) {
        Map<String, List<String>> tagMap = new HashMap<>();
        for (GrayTagItem item : items) {
            for (Map.Entry<String, String> envEntry : item.getEnvTag().entrySet()) {
                resetMapInfo(tagMap, envEntry);
            }
            for (Map.Entry<String, String> entry : item.getTrafficTag().entrySet()) {
                resetMapInfo(tagMap, entry);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<String>> envEntry : tagMap.entrySet()) {
            if (builder.length() > 0) {
                builder.append(" and ");
            }
            builder.append("( ")
                .append(envEntry.getKey())
                .append(" not in ")
                .append(getStrForSets(new HashSet<>(envEntry.getValue())))
                .append(")");
        }
        return builder.length() > 0 ? builder.toString() : "";
    }

    private static void resetMapInfo(Map<String, List<String>> sourceMap, Map.Entry<String, String> entry) {
        if (sourceMap.containsKey(entry.getKey())) {
            sourceMap.get(entry.getKey()).add(entry.getValue());
        } else {
            List<String> list = new ArrayList<>();
            list.add(entry.getValue());
            sourceMap.put(entry.getKey(), list);
        }
    }

    private static String removeMseGrayTagFromOriginSubData(String originSubData) {
        if (StringUtils.isBlank(originSubData)) {
            return originSubData;
        }
        String[] originConditions = pattern.split(originSubData);
        List<String> refactorConditions = new ArrayList<>();
        for (String condition: originConditions) {
            if (containsGrayTags(condition)) {
                refactorConditions.add(condition);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < refactorConditions.size(); i++) {
            sb.append(refactorConditions.get(i));
            if (i != refactorConditions.size() - 1) {
                sb.append(" AND ");
            }
        }
        return sb.toString();
    }

    private static boolean containsGrayTags(String condition) {
        Set<String> tagKeys
            = MqGrayscaleConfigUtils.buildGrayTagKeySet(MqGrayscaleConfigUtils.getGrayscaleConfigs().getGrayscale());
        for (String key : tagKeys) {
            if (condition.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public static void resetsSQL92SubscriptionData(SubscriptionData subscriptionData) {
        String originSubData = buildSQL92ExpressionByTags(subscriptionData.getTagsSet());
        String subStr = addMseGrayTagsToSQL92Expression(originSubData);
        if (StringUtils.isEmpty(subStr)) {
            subStr = "( " + MqGrayscaleConfigUtils.MICRO_SERVICE_GRAY_TAG_KEY + "  is null ) or ( "
                    + MqGrayscaleConfigUtils.MICRO_SERVICE_GRAY_TAG_KEY + "  is not null )";
        }
        subscriptionData.setExpressionType("SQL92");
        subscriptionData.getTagsSet().clear();
        subscriptionData.getCodeSet().clear();
        subscriptionData.setSubString(subStr);
        subscriptionData.setSubVersion(System.currentTimeMillis());
        LOGGER.warning(String.format(Locale.ENGLISH, "update TAG to SQL92 subscriptionData, originSubStr: [%s], "
            + "newSubStr: [%s]", originSubData, subStr));
    }

    public static void resetAutoCheckGrayTagItems(List<GrayTagItem> grayTagItems) {
        AUTO_CHECK_GRAY_TAGS.clear();
        MqGrayscaleConfigUtils.MQ_GRAY_TAGS_CHANGE_FLAG = true;
        if (!grayTagItems.isEmpty()) {
            AUTO_CHECK_GRAY_TAGS.put(AUTO_CHECK_GRAY_TAG_KEY, grayTagItems);
        }
    }

    public static List<GrayTagItem> getGrayTags() {
        return AUTO_CHECK_GRAY_TAGS.get(AUTO_CHECK_GRAY_TAG_KEY);
    }
}
