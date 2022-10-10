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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;

import com.huawei.discovery.entity.ServiceInstance;

public class HttpParamersUtils {

    public static String buildNewUrl(URI uri, ServiceInstance serviceInstance, String path, String method) {
        StringBuilder urlBuild = new StringBuilder();
        urlBuild.append(uri.getScheme())
                .append(HttpConstants.HTTP_URL_DOUBLIE_SLASH)
                .append(serviceInstance.getIp())
                .append(HttpConstants.HTTP_URL_COLON)
                .append(serviceInstance.getPort())
                .append(path);
        if (method.equals(HttpConstants.HTTP_GET)) {
            urlBuild.append(HttpConstants.HTTP_URL_UNKNOWN)
                    .append(uri.getQuery());
        }
        return urlBuild.toString();
    }

    public static Map<String, String> recoverHostAndPath(String path) {
        Map<String, String> result = new HashMap<String, String>();
        if (StringUtils.isEmpty(path)) {
            return result;
        }
        int startIndex = 0;
        while (startIndex < path.length() && path.charAt(startIndex) == HttpConstants.HTTP_URL_SINGLE_SLASH) {
            startIndex++;
        }
        String tempPath = path.substring(startIndex);
        result.put(HttpConstants.HTTP_URI_HOST, tempPath.substring(0, tempPath.indexOf(HttpConstants.HTTP_URL_SINGLE_SLASH)));
        result.put(HttpConstants.HTTP_URI_PATH, tempPath.substring(tempPath.indexOf(HttpConstants.HTTP_URL_SINGLE_SLASH)));
        return result;
    }

    public static HttpUriRequest builNewRequest(String uriNew, String method, HttpUriRequest httpUriRequest) {
        if (HttpConstants.HTTP_GET.equals(method)) {
            return new HttpGet(uriNew);
        } else if (HttpConstants.HTTP_POST.equals(method)){
            HttpPost oldHttpPost = (HttpPost) httpUriRequest;
            HttpPost httpPost = new HttpPost(uriNew);
            httpPost.setEntity(oldHttpPost.getEntity());
            return httpPost;
        }
        return httpUriRequest;
    }
}
