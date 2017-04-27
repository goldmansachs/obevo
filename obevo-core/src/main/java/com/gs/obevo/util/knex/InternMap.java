/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevo.util.knex;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Description: A repository for interned data - saves memory by serving out references to shared
 * instances of immutable objects - Strings in particular and also zero value numerics.
 * The zero value in numeric data is very inputreader but other values will rarely coincide
 * so only zero is interned for these objects.
 */
public final class InternMap {
    private static final int SEGMENTS = 64;               // Must be a power of 2. Represents likely concurrency.
    private static final int SEGMENT_SHIFT = 26;               // 32 minus number of bits representing segment value
    private static final InternMap instance = new InternMap();
    private static final Integer zeroInteger = Integer.valueOf(0);
    private static final Long zeroLong = Long.valueOf(0);
    private static final Float zeroFloat = Float.valueOf(0);
    private static final Double zeroDouble = Double.valueOf(0);
    private static final BigDecimal zeroDecimal = BigDecimal.ZERO;

    private final WeakHashMap<String, WeakReference<String>>[] stringCache;
    private final WeakHashMap<Timestamp, WeakReference<Timestamp>>[] timestampCache;
    private final AtomicLong[] accesses;
    private final AtomicLong[] misses;

    /**
     * Singleton pattern - we really want to have only one cache per JVM
     *
     * @return The one and only instance of InternMap
     */
    public static InternMap instance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    InternMap() {
        this.stringCache = new WeakHashMap[SEGMENTS];
        this.timestampCache = new WeakHashMap[SEGMENTS];
        this.accesses = new AtomicLong[SEGMENTS];
        this.misses = new AtomicLong[SEGMENTS];
        for (int i = 0; i < SEGMENTS; i++) {
            this.stringCache[i] = new WeakHashMap<String, WeakReference<String>>();
            this.timestampCache[i] = new WeakHashMap<Timestamp, WeakReference<Timestamp>>();
            this.accesses[i] = new AtomicLong();
            this.misses[i] = new AtomicLong();
        }
    }

    /**
     * Store a String in the internal cache if not already present and return a reference to the instance held in the
     * cache.
     *
     * @param s The String to intern
     * @return The instance now held in the cache
     */
    public String intern(String s) {
        if (s == null) {
            return s;
        }
        int segment = s.hashCode() >>> SEGMENT_SHIFT;
        synchronized (this.stringCache[segment]) {
            this.accesses[segment].incrementAndGet();
            String retVal = null;
            WeakReference<String> ref = this.stringCache[segment].get(s);
            if (ref != null) {
                retVal = ref.get();
            }
            if (retVal == null) {
                retVal = s;
                this.misses[segment].incrementAndGet();
                this.stringCache[segment].put(s, new WeakReference<String>(s));
            }
            return retVal;
        }
    }

    /**
     * Store a LocalDate in the internal cache if not already present and return a reference to the instance held in the
     * cache.
     *
     * @param d The LocalDate to intern
     * @return The instance now held in the cache
     */
    public Timestamp intern(Timestamp d) {
        if (d == null) {
            return d;
        }
        int segment = d.hashCode() >>> SEGMENT_SHIFT;
        synchronized (this.timestampCache[segment]) {
            this.accesses[segment].incrementAndGet();
            Timestamp retVal = null;
            WeakReference<Timestamp> ref = this.timestampCache[segment].get(d);
            if (ref != null) {
                retVal = ref.get();
            }
            if (retVal == null) {
                retVal = d;
                this.misses[segment].incrementAndGet();
                this.timestampCache[segment].put(d, new WeakReference<Timestamp>(d));
            }
            return retVal;
        }
    }

    /**
     * Convenience method for Object which checks the run-time type and delegates to the appropriate method.
     * Useful to support future additional object types without necessarily having to change client code.
     *
     * @param o The object to intern
     * @return The instance now held in the cache
     */
    public Object intern(Object o) {
        if (o instanceof String) {
            return this.intern((String) o);
        } else if (o instanceof BigDecimal) {
            return this.intern((BigDecimal) o);
        } else if (o instanceof Double) {
            return this.intern((Double) o);
        } else if (o instanceof Float) {
            return this.intern((Float) o);
        } else if (o instanceof Long) {
            return this.intern((Long) o);
        } else if (o instanceof Integer) {
            return this.intern((Integer) o);
        } else {
            return o;
        }
    }

    /**
     * Return a reference to a shared Integer object if the cache holds one of the same value as the Integer passed in.
     * Currently the only Integer cached is zero.
     *
     * @param i The Integer to intern
     * @return A reference to a shared Integer object if the cache holds one of the same value as the Integer passed in.
     */
    public Integer intern(Integer i) {
        if (i == null) {
            return i;
        } else if (i.equals(zeroInteger)) {
            return zeroInteger;
        } else {
            return Integer.valueOf(i);
        }
    }

    /**
     * Return a reference to a shared Long object if the cache holds one of the same value as the Long passed in.
     * Currently the only Long cached is zero.
     *
     * @param l The Long to intern
     * @return A reference to a shared Long object if the cache holds one of the same value as the Long passed in.
     */
    public Long intern(Long l) {
        if (l == null) {
            return l;
        } else if (l.equals(zeroLong)) {
            return zeroLong;
        } else {
            return Long.valueOf(l);
        }
    }

    /**
     * Return a reference to a shared Float object if the cache holds one of the same value as the Float passed in.
     * Currently the only Float cached is zero.
     *
     * @param f The Float to intern
     * @return A reference to a shared Float object if the cache holds one of the same value as the Float passed in.
     */
    public Float intern(Float f) {
        if (f == null) {
            return f;
        } else if (f.equals(zeroFloat)) {
            return zeroFloat;
        } else {
            return Float.valueOf(f);
        }
    }

    /**
     * Return a reference to a shared Double object if the cache holds one of the same value as the Double passed in.
     * Currently the only Double cached is zero.
     *
     * @param d The Double to intern
     * @return A reference to a shared Double object if the cache holds one of the same value as the Double passed in.
     */
    public Double intern(Double d) {
        if (d == null) {
            return d;
        } else if (d.equals(zeroDouble)) {
            return zeroDouble;
        } else {
            return Double.valueOf(d);
        }
    }

    /**
     * Return a reference to a shared BigDecimal object if the cache holds one of the same value as the BigDecimal
     * passed in.
     * Currently the only BigDecimal cached is zero.
     *
     * @param d The BigDecimal to intern
     * @return A reference to a shared BigDecimal object if the cache holds one of the same value as the BigDecimal
     * passed in.
     */
    public BigDecimal intern(BigDecimal d) {
        if (d == null) {
            return d;
        } else if (d.equals(zeroDecimal)) {
            return zeroDecimal;
        } else {
            return d;
        }
    }

    /**
     * Show a summary of the cache state including current size and hit and miss statistics.
     */
    @Override
    public synchronized String toString() {
        StringBuilder s = new StringBuilder();
        long totalSize = 0;
        long totalAccesses = 0;
        long totalHits = 0;
        long totalMisses = 0;
        for (int i = 0; i < SEGMENTS; i++) {
            synchronized (this.stringCache[i]) {
                synchronized (this.timestampCache[i]) {
                    totalSize += this.stringCache[i].size() + this.timestampCache[i].size();
                    totalAccesses += this.accesses[i].get();
                    totalHits += this.accesses[i].get() - this.misses[i].get();
                    totalMisses += this.misses[i].get();
                }
            }
        }
        s.append("CacheSize=").append(totalSize);
        s.append(" Accesses=").append(totalAccesses);
        s.append(" Hits=").append(totalHits);
        s.append(" Misses=").append(totalMisses);
        return s.toString();
    }
}
