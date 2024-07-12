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

import io.sermant.core.utils.StringUtils;

import org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionDataUtils.class);

    private static final String CONSUME_TYPE_ALL = "all";

    private static final String CONSUME_TYPE_BASE = "base";

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
        if (StringUtils.isEmpty(MqGrayscaleConfigUtils.getGrayEnvTag())) {
            // all模式所有消息返回
            if (CONSUME_TYPE_ALL.equals(MqGrayscaleConfigUtils.getConsumeType())) {
                return "";
            }
            // base模式只返回不带标签消息
            if (CONSUME_TYPE_BASE.equals(MqGrayscaleConfigUtils.getConsumeType())) {
                sb.append(" ( ( ")
                        .append(MqGrayscaleConfigUtils.MICRO_SERVICE_GRAY_TAG_KEY)
                        .append(" is null ")
                        .append(" )")
                        .append(" and ( ")
                        .append(MqGrayscaleConfigUtils.MICRO_TRAFFIC_GRAY_TAG_KEY)
                        .append(" is null ) ")
                        .append(" ) ");
                return sb.toString();
            }
            // auto模式下如果存在流量标签，则仅消费流量标签消息
            if (!StringUtils.isEmpty(MqGrayscaleConfigUtils.getTrafficGrayTag())) {
                Set<String> trafficSet = new HashSet<>();
                trafficSet.add(MqGrayscaleConfigUtils.getTrafficGrayTag());
                String trafficGrayTag = getStrForSets(trafficSet);
                sb.append(" ( ")
                        .append(MqGrayscaleConfigUtils.MICRO_TRAFFIC_GRAY_TAG_KEY)
                        .append(" in ")
                        .append(trafficGrayTag)
                        .append(" )");
                return sb.toString();
            }
            sb.append(" ( ( ")
                    .append(MqGrayscaleConfigUtils.MICRO_SERVICE_GRAY_TAG_KEY)
                    .append(" not in ")
                    .append(getStrForSets(MqGrayscaleConfigUtils.getExcludeTagsForSet()))
                    .append(" )")
                    .append(" or ( ")
                    .append(MqGrayscaleConfigUtils.MICRO_TRAFFIC_GRAY_TAG_KEY)
                    .append(" not in ")
                    .append(getStrForSets(MqGrayscaleConfigUtils.getExcludeTagsForSet()))
                    .append(" ) ");
        } else {
            Set<String> set = new HashSet<>();
            set.add(MqGrayscaleConfigUtils.getGrayEnvTag());
            String envGrayTag = getStrForSets(set);
            sb.append(" ( ( ")
                    .append(MqGrayscaleConfigUtils.MICRO_SERVICE_GRAY_TAG_KEY)
                    .append(" in ")
                    .append(envGrayTag)
                    .append(")")
                    .append(" or ( ")
                    .append(MqGrayscaleConfigUtils.MICRO_TRAFFIC_GRAY_TAG_KEY)
                    .append(" is not null ) ")
                    .append(" ) ");
        }
        return sb.toString();
    }

    private static String removeMseGrayTagFromOriginSubData(String originSubData) {
        if (StringUtils.isBlank(originSubData)) {
            return originSubData;
        }
        String[] originConditions = pattern.split(originSubData);
        List<String> refactorConditions = new ArrayList<>();
        for (String condition: originConditions) {
            if (!condition.contains(MqGrayscaleConfigUtils.MICRO_SERVICE_GRAY_TAG_KEY)
                    && !condition.contains(MqGrayscaleConfigUtils.MICRO_TRAFFIC_GRAY_TAG_KEY)) {
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
        LOGGER.warn("update TAG to SQL92 subscriptionData, originSubStr: {}, newSubStr: {}", originSubData, subStr);
    }
}
