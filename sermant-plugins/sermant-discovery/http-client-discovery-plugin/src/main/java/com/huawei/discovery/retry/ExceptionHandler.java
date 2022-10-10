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

package com.huawei.discovery.retry;

import java.util.function.Function;

/**
 * 异常处理器, 需要拦截处对异常进行封装处理
 *
 * @author zhouss
 * @since 2022-09-28
 */
public interface ExceptionHandler {
    /**
     * 若调用发生异常, 则需调用其对异常进行封装, 返回给上游或者调用端
     *
     * @param func 异常封装器, 输入异常, 需转换为对应的异常结果
     * @return 封装后的异常结果
     */
    Object handle(Function<Exception, Object> func);
}
