/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.state.forst;

import org.apache.flink.core.asyncprocessing.InternalAsyncFuture;

import java.io.IOException;
import java.util.List;

/**
 * The Get access request for ForStDB.
 *
 * @param <K> The type of key in get access request.
 * @param <N> The type of namespace in get access request.
 * @param <V> The type of value returned by get request.
 */
public class ForStDBRawGetRequest<K, N, V> extends ForStDBGetRequest<K, N, List<V>, byte[]> {

    ForStDBRawGetRequest(
            ContextKey<K, N> key,
            ForStInnerTable<K, N, List<V>> table,
            InternalAsyncFuture<byte[]> future) {
        super(key, table, future);
    }

    @Override
    public void completeStateFuture(byte[] bytesValue) throws IOException {
        future.complete(bytesValue);
    }
}
