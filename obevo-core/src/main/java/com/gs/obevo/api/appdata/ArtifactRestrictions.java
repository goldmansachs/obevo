/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevo.api.appdata;

import java.util.regex.Pattern;

import com.gs.obevo.util.RegexUtil;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

public abstract class ArtifactRestrictions implements Predicate<Environment> {

    private final MutableSet<String> includes;
    private final MutableSet<String> excludes;

    ArtifactRestrictions(MutableSet<String> includes, MutableSet<String> excludes) {
        this.includes = includes == null ? UnifiedSet.<String>newSet() : includes;
        this.excludes = excludes == null ? UnifiedSet.<String>newSet() : excludes;

        if (!this.includes.isEmpty() && !this.excludes.isEmpty()) {
            throw new IllegalArgumentException("Cannot specify both include and exclude");
        }
    }

    public MutableSet<String> getIncludes() {
        return this.includes;
    }

    public MutableSet<String> getExcludes() {
        return this.excludes;
    }

    @Override
    public boolean accept(Environment env) {
        if (!includes.isEmpty()) {
            return matches(includes, getRestrictedValue(env));
        } else if (!excludes.isEmpty()) {
            return !matches(excludes, getRestrictedValue(env));
        } else {
            return true;
        }
    }

    private boolean matches(MutableSet<String> patterns, String value) {
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(RegexUtil.convertWildcardPatternToRegex(pattern));
            if (p.matcher(value).matches()) {
                return true;
            }
        }
        return false;
    }

    protected abstract String getRestrictedValue(Environment env);

    public static Predicate2<Restrictable, Environment> apply() {
        return new Predicate2<Restrictable, Environment>() {
            @Override
            public boolean accept(Restrictable restrictable, Environment env) {
                ImmutableList<ArtifactRestrictions> restrictions = restrictable.getRestrictions();
                if (restrictions == null) {
                    return true;
                }

                return Predicates.and(restrictions).accept(env);
            }
        };
    }
}
