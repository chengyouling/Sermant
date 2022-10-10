/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.discovery.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 参数标签类
 *
 * @author chengyouling
 * @since 2022-09-14
 */
public class HttpConstants {

    public static final String HTTP_URI_HOST = "host";

    public static final String HTTP_URI_PATH = "path";

    public static final String HTTP_GET = "GET";

    public static final String HTTP_POST = "POST";

    public static final String HTTP_URL_DOUBLIE_SLASH = "://";

    public static final String HTTP_URL_COLON = ":";

    public static final String HTTP_URL_UNKNOWN = "?";

    public static final char HTTP_URL_SINGLE_SLASH = '/';

    public static final String HTTP_URL_SCHEME = "scheme";

    public static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String currentTime() {
        return simpleDateFormat.format(new Date());
    }
}
