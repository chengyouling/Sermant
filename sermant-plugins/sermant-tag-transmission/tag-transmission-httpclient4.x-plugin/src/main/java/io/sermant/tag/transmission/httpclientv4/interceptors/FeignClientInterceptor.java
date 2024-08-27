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

package io.sermant.tag.transmission.httpclientv4.interceptors;

import feign.Request;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.AbstractInterceptor;
import io.sermant.core.utils.CollectionUtils;
import io.sermant.core.utils.LogUtils;
import io.sermant.core.utils.tag.TrafficUtils;
import io.sermant.tag.transmission.config.strategy.TagKeyMatcher;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The client enhancement class， initiates the feign request method
 *
 * @author provenceee
 * @since 2022-07-12
 */
public class FeignClientInterceptor extends AbstractInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public ExecuteContext before(ExecuteContext context) {
        LogUtils.printHttpRequestBeforePoint(context);
        Object argument = context.getArguments()[0];
        if (argument instanceof Request) {
            Request request = (Request) argument;
            for (Map.Entry<String, List<String>> entry : TrafficUtils.getTrafficTag().getTag().entrySet()) {
                String key = entry.getKey();
                if (!TagKeyMatcher.isMatch(key)) {
                    continue;
                }
                List<String> values = entry.getValue();

                // The server side converts the label value to list storage when it is not null. If it is null, it directly
                // puts null. Therefore, if the client side values are empty, they must be null.
                if (CollectionUtils.isEmpty(values)) {
                    request.header(key, "");
                    LOGGER.log(Level.FINE, "Traffic tag {0} have been injected to feign client.", entry);
                    continue;
                }
                for (String value : values) {
                    request.header(key, value);
                }
                LOGGER.log(Level.FINE, "Traffic tag {0}={1} have been injected to feign client.", new Object[]{key,
                        values});
            }
        }
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) {
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) {
        return context;
    }
}