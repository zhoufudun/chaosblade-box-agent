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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * A fixed-capacity ordered list backed by LinkedList with thread-safe operations.
 * When capacity is exceeded, the oldest element (front) is removed.
 * Mirrors Go's pkg/tools/limitedlist.go.
 *
 * @param <T> the element type
 */
public class LimitedList<T> {

    private static final Logger logger = LoggerFactory.getLogger(LimitedList.class);

    private final int capacity;
    private final LinkedList<T> list;
    private final Object lock = new Object();

    /**
     * Create a new LimitedList with the given capacity.
     *
     * @param capacity the maximum number of elements; must be > 0
     * @throws IllegalArgumentException if capacity <= 0
     */
    public LimitedList(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than 0");
        }
        this.capacity = capacity;
        this.list = new LinkedList<>();
    }

    /**
     * Add an element to the back of the list. If the list exceeds capacity,
     * the front element is removed.
     */
    public void put(T value) {
        synchronized (lock) {
            list.addLast(value);
            if (list.size() > capacity) {
                list.removeFirst();
            }
        }
    }

    /**
     * Iterate forward through the list. A snapshot is taken under the lock,
     * then the handler is called outside the lock.
     *
     * @param handler        the handler to call for each element
     * @param breakWhenWrong if true, stop iteration on the first error
     */
    public void foreach(ElementHandler<T> handler, boolean breakWhenWrong) {
        List<T> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(list);
        }
        for (T element : snapshot) {
            try {
                handler.handle(element);
            } catch (Exception e) {
                if (!e.getMessage().contains("nolog")) {
                    logger.warn("handle wrong: {}", e.getMessage());
                }
                if (breakWhenWrong) {
                    break;
                }
            }
        }
    }

    /**
     * Iterate in reverse through the list. A snapshot is taken under the lock,
     * then the handler is called outside the lock.
     *
     * @param handler        the handler to call for each element
     * @param breakWhenWrong if true, stop iteration on the first error
     */
    public void foreachReverse(ElementHandler<T> handler, boolean breakWhenWrong) {
        List<T> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(list);
        }
        ListIterator<T> it = snapshot.listIterator(snapshot.size());
        while (it.hasPrevious()) {
            T element = it.previous();
            try {
                handler.handle(element);
            } catch (Exception e) {
                if (!e.getMessage().contains("nolog")) {
                    logger.warn("handle wrong: {}", e.getMessage());
                }
                if (breakWhenWrong) {
                    break;
                }
            }
        }
    }

    /**
     * Return the current size of the list.
     */
    public int size() {
        synchronized (lock) {
            return list.size();
        }
    }

    /**
     * Handler interface for iterating elements.
     */
    public interface ElementHandler<T> {
        void handle(T value) throws Exception;
    }
}
