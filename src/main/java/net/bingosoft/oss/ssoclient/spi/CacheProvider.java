/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.bingosoft.oss.ssoclient.spi;

public interface CacheProvider {
    /**
     * 根据传入的key获取已缓存的对象
     */
    <T> T get(String key);

    /**
     * 根据传入的key和item缓存对象item，这里expires是缓存过期时间，在缓存过期后需要清理缓存
     */
    void put(String key, Object item, long expires);

    /**
     * 根据key将缓存的对象清除
     */
    void remove(String key);
}