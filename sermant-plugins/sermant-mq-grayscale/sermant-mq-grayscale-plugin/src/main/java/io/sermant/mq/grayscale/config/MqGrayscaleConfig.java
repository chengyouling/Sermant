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

import io.sermant.core.plugin.config.PluginConfig;

/**
 * mqGrayscaleConfig entry
 *
 * @author chengyouling
 * @since 2024-05-27
 **/
public class MqGrayscaleConfig {
    private boolean enabled = false;

    private List<GrayTagItem> grayscale = new ArrayList<>();

    private BaseMessage base;

    private boolean serverGrayEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BaseMessage getBase() {
        return base;
    }

    public void setBase(BaseMessage base) {
        this.base = base;
    }

    public boolean isServerGrayEnabled() {
        return serverGrayEnabled;
    }

    public void setServerGrayEnabled(boolean serverGrayEnabled) {
        this.serverGrayEnabled = serverGrayEnabled;
    }

    public List<GrayTagItem> getGrayscale() {
        return grayscale;
    }

    public void setGrayscale(List<GrayTagItem> grayscale) {
        this.grayscale = grayscale;
    }
}
