/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.boardgamegeek.util

import android.os.SystemClock
import androidx.collection.ArrayMap

import java.util.concurrent.TimeUnit

/**
 * Utility class that decides whether we should fetch some data or not.
 */
class RateLimiter<in KEY>(timeout: Int, timeUnit: TimeUnit) {
    private val timestamps = ArrayMap<KEY, Long>()
    private val timeout = timeUnit.toMillis(timeout.toLong())

    @Synchronized
    fun shouldProcess(key: KEY, now: Long = SystemClock.uptimeMillis()): Boolean {
        val lastFetched = timestamps[key]
        return if ((lastFetched == null) || (now - lastFetched > timeout)) {
            timestamps[key] = now
            true
        } else false
    }

    //    @Synchronized
    //    @Suppress("unused")
    //    fun willProcessAt(key: KEY): Long {
    //        val lastFetched = timestamps[key]
    //        val now = SystemClock.uptimeMillis()
    //        return if ((lastFetched == null) || (now - lastFetched > timeout)) {
    //            now
    //        } else lastFetched + timeout
    //    }

    @Synchronized
    fun reset(key: KEY) {
        timestamps.remove(key)
    }
}
