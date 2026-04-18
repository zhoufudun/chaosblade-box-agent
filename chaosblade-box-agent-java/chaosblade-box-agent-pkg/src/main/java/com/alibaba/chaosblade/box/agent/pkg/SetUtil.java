/*
 * Copyright 2025 The ChaosBlade Authors
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

package com.alibaba.chaosblade.box.agent.pkg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple HashMap-based set.
 * Mirrors Go's pkg/tools/set.go.
 *
 * @param <T> the element type
 */
public class SetUtil<T> {

    private final Map<T, Boolean> map;

    public SetUtil() {
        this.map = new HashMap<>();
    }

    public void add(T item) {
        map.put(item, Boolean.TRUE);
    }

    public void remove(T item) {
        map.remove(item);
    }

    public boolean contains(T item) {
        return map.containsKey(item);
    }

    public int length() {
        return map.size();
    }

    public void clear() {
        map.clear();
    }

    public List<T> keys() {
        return new ArrayList<>(map.keySet());
    }

    /**
     * Return keys as a list of strings (assumes T is String).
     */
    @SuppressWarnings("unchecked")
    public List<String> stringKeys() {
        List<String> result = new ArrayList<>(map.size());
        for (T key : map.keySet()) {
            result.add((String) key);
        }
        return result;
    }
}
