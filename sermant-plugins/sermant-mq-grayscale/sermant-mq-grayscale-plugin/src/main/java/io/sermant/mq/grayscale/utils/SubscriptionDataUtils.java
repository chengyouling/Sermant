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

public class SubscriptionDataUtils {
    public static final String EXPRESSION_TYPE_TAG = "TAG";

    public static final String EXPRESSION_TYPE_SQL92 = "SQL92";

    private static final Pattern pattern = Pattern.compile("and|or", Pattern.CASE_INSENSITIVE);

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionDataUtils.class);

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
            if (MqGrayscaleConfigUtils.getExcludeTagsForSet().isEmpty()) {
                return "";
            }
            sb.append(" ( ( ")
                    .append(MqGrayscaleConfigUtils.MICRO_SERVICE_GRAY_TAG_KEY)
                    .append(" not in ")
                    .append(getStrForSets(MqGrayscaleConfigUtils.getExcludeTagsForSet()))
                    .append(" )")
                    .append(" or ( ")
                    .append(MqGrayscaleConfigUtils.MICRO_SERVICE_GRAY_TAG_KEY)
                    .append(" is null ) ")
                    .append(" ) ");
        } else {
            Set<String> set = new HashSet<>();
            set.add(MqGrayscaleConfigUtils.getGrayEnvTag());
            String envGrayTag = getStrForSets(set);
            sb.append(" ( ")
                    .append(MqGrayscaleConfigUtils.MICRO_SERVICE_GRAY_TAG_KEY)
                    .append(" in ")
                    .append(envGrayTag)
                    .append(")");
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
            if (!condition.contains(MqGrayscaleConfigUtils.MICRO_SERVICE_GRAY_TAG_KEY)) {
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
        String originSubData = SubscriptionDataUtils.buildSQL92ExpressionByTags(subscriptionData.getTagsSet());
        String subStr = SubscriptionDataUtils.addMseGrayTagsToSQL92Expression(originSubData);
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
