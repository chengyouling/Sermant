/*
 * Copyright (C) 2021-2021 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.recordconsole.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * zookeeper中groovy脚本信息
 *
 * @author lilai
 * @version 0.0.1
 * @since 2021-04-13
 */
@Getter
@Setter
public class GroovyInfoEntity {
    /**
     * 脚本资源URL
     */
    private String url;

    /**
     * 脚本文件名（带后缀）
     */
    private String scriptName;

    /**
     * 脚本脱敏流程函数名
     */
    private String functionName;
}
