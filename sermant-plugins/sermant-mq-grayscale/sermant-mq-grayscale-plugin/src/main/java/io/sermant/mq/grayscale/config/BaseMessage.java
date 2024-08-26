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

package io.sermant.mq.grayscale.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * base consumeType entry
 *
 * @author chengyouling
 * @since 2024-05-27
 **/
public class BaseMessage {
    private String consumeType;

    private long autoCheckDelayTime;

    private List<String> excludeGroupTags = new ArrayList<>();

    public String getConsumeType() {
        return consumeType == null ? "" : consumeType.toLowerCase(Locale.ROOT);
    }

    public void setConsumeType(String consumeType) {
        this.consumeType = consumeType;
    }

    public long getAutoCheckDelayTime() {
        return autoCheckDelayTime;
    }

    public void setAutoCheckDelayTime(long autoCheckDelayTime) {
        this.autoCheckDelayTime = autoCheckDelayTime;
    }

    public List<String> getExcludeGroupTags() {
        return excludeGroupTags;
    }

    public void setExcludeGroupTags(List<String> excludeGroupTags) {
        this.excludeGroupTags = excludeGroupTags;
    }
}
