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

import java.util.HashMap;
import java.util.Map;

/**
 * messageFilter entry
 *
 * @author chengyouling
 * @since 2024-05-27
 **/
public class MessageFilter {
    private String consumeType;

    private long autoCheckDelayTime;

    private Map<String, String> excludeTags = new HashMap<>();

    public String getConsumeType() {
        return consumeType;
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

    public Map<String, String> getExcludeTags() {
        return excludeTags;
    }

    public void setExcludeTags(Map<String, String> excludeTags) {
        this.excludeTags = excludeTags;
    }

    public boolean isExcludeTagsConfigChanged(MessageFilter compare) {
        if (this.excludeTags.size() != compare.getExcludeTags().size()) {
            return true;
        }
        for (Map.Entry<String, String> entry : this.excludeTags.entrySet()) {
            if (!compare.getExcludeTags().containsKey(entry.getKey())
                || !compare.getExcludeTags().get(entry.getKey()).equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }
}
