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

package com.huawei.javamesh.core.lubanops.bootstrap.api;

/**
 * @author
 * @date 2020/12/17 14:35
 */
public interface CircuitBreaker {

    /**
     * allow request pass?
     *
     * @return
     */
    public boolean allowRequest();

    /**
     * is circuit open
     *
     * @return
     */
    public boolean isOpen();

    /**
     * mark success.
     */
    void markSuccess();

    /**
     * mark failure.
     */
    void markFailure();
}
