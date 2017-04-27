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
package com.gs.obevo.api.platform;

import java.sql.Timestamp;

import com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * Represent the platform/system whose code we want to maintain.
 *
 * In terms of extensibility - the main deployment logic revolves around this class. If you ask the question of "how can
 * I build an implementation against 'system ABC'", then start w/ this class.
 *
 * Obevo is meant to deploy systems that fit the "incremental deployment" type.
 * TODO write up more information on the incremental pattern.
 */
public interface Platform {
    /**
     * The name of the this {@link ChangeType}. This value should be leveraged where possible to interact
     * with the wider ecosystem, e.g. change restrictions, directory structure for code.
     */
    String getName();

    /**
     * The kinds of {@link ChangeType} that are supported by this {@link Platform}.
     */
    ImmutableList<ChangeType> getChangeTypes();

    /**
     * Returns the {@link ChangeType} registered to this platform - the name provided should correspond to {@link ChangeType#getName()}.
     *
     * Throws an exception if no such name is registered.
     */
    ChangeType getChangeType(String name);

    /**
     * Checks if the platform has a ChangeType of the given name already registered. Should only be used in exceptional
     * cases - usually, we want to be able to rely on {@link #getChangeType(String)}.
     */
    boolean hasChangeType(String name);

    /**
     * Whether the change calculation for drops requires that the objects be dropped in order. This is usually false
     * for platforms - a couple exceptional cases require this.
     */
    boolean isDropOrderRequired();

    /**
     * Function for converting the requested DB object name to the type the db expects,
     * as to allow for user convenience and facilitate executing some sqls from one db to another
     * e.g. some DBs are case-sensitive; hence, nothing should be changed from what the user gives.
     * others are case-insensitive, but return results as all-capitalized
     * others are case-insensitive, but return results as all-lowercase
     */
    Function<String, String> convertDbObjectName();

    ObjectTypeAndNamePredicateBuilder getObjectExclusionPredicateBuilder();

    /**
     * Given an object from the result set returned by the MapListHandler in JdbcHelper, this will return a long value.
     * This is simple for most DBMSs, but a few need special handling.
     */
    Long getLongValue(Object obj);

    /**
     * Given an object from the result set returned by the MapListHandler in JdbcHelper, this will return a timestamp value.
     * This is simple for most DBMSs, but a few need special handling.
     */
    Timestamp getTimestampValue(Object obj);

    /**
     * Given an object from the result set returned by the MapListHandler in JdbcHelper, this will return an integer value.
     * This is simple for most DBMSs, but a few need special handling.
     */
    Integer getIntegerValue(Object obj);
}
