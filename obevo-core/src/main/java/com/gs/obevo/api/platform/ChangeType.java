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

import com.gs.obevo.impl.text.TextDependencyExtractor;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;

/**
 * Represents a kind of object to be maintained in a {@link Platform}. This defines the general parameters of how to
 * maintain this object type in code, e.g.
 *  * <ul>
 *     <li>How to deploy / undeploy it</li>
 *     <li>How to maintain it in code</li>
 *     <li>How to sort and treat its dependencies.</li>
 * </ul>
 *
 * Note that this extends {@link ChangeTypeBehavior} - the idea here is that some methods require a live application
 * context to be injected, so those methods are on the {@link ChangeTypeBehavior} class and injected into this. At the
 * same time - from a client friendliness view, we don't want to have them deal w/ two classes; hence, we have these
 * two classes work together to give the best of both words to the client.
 */
public interface ChangeType {

    // TODO these constants should get moved out into the implementation-specific modules
    String TABLE_STR = "TABLE";
    String STATICDATA_STR = "STATICDATA";
    String FOREIGN_KEY_STR = "FOREIGN_KEY";
    /**
     * DB trigger objects. This will be the new rerunnable ChangeType.
     */
    String TRIGGER_STR = "TRIGGER2";  // must be named trigger2 while the old name exists
    String PACKAGE_STR = "PACKAGE";  // must be named trigger2 while the old name exists
    String PACKAGE_BODY = "PACKAGE_BODY";  // must be named trigger2 while the old name exists
    /**
     * DB trigger objects. This is the old incrementable change type
     *
     * @deprecated Move to the new rerunnable trigger ChangeType {@link #TRIGGER_STR}
     */
    @Deprecated
    String TRIGGER_INCREMENTAL_OLD_STR = "TRIGGER";  // need to keep the old name for backwards-compatibility
    String MIGRATION_STR = "MIGRATION";
    String VIEW_STR = "VIEW";
    String SP_STR = "SP";
    String FUNCTION_STR = "FUNCTION";
    String SEQUENCE_STR = "SEQUENCE";

    String INDEX_STR = "INDEX";
    String UNCLASSIFIED_STR = "UNCLASSIFIED";

    // TODO the following constants are current DBMS-specific; ideally shouldn't  have to be here
    String DEFAULT_STR = "DEFAULT";
    String RULE_STR = "RULE";
    String USERTYPE_STR = "USERTYPE";
    String AVRO_SCHEMA_STR = "AVRO_SCHEMA";

    Function<ChangeType, Integer> TO_DEPLOY_ORDER_PRIORITY = new Function<ChangeType, Integer>() {
        @Override
        public Integer valueOf(ChangeType object) {
            return object.getDeployOrderPriority();
        }
    };

    Predicate<ChangeType> IS_RERUNNABLE = new Predicate<ChangeType>() {
        @Override
        public boolean accept(ChangeType object) {
            return object.isRerunnable();
        }
    };

    Function<ChangeType, String> TO_NAME = new Function<ChangeType, String>() {
        @Override
        public String valueOf(ChangeType object) {
            return object.getName();
        }
    };

    /**
     * The name of the this {@link ChangeType}. This value should be leveraged where possible to interact
     * with the wider ecosystem, e.g. change restrictions, directory structure for code.
     */
    String getName();

    /**
     * Deploy order priority to serve as a tiebreaker in the topological sorting.
     */
    int getDeployOrderPriority();

    /**
     * Whether the change type is rerunnable or incremental. This is a key differentiator in the deploy behavior for
     * this ChangeType.
     */
    boolean isRerunnable();

    /**
     * The recommended directory name for clients to store code of this type in. Should generally correspond to
     * {@link #getName()}.
     */
    String getDirectoryName();

    /**
     * The legacy directory name that is checked for this change type. This is used as a fallback to the regular
     * {@link #getDirectoryName()}.
     * @deprecated Would like to wean folks off of this
     */
    @Deprecated
    String getDirectoryNameOld();

    /**
     * Whether changes of this type should have their text code analyzed for dependencies (see {@link TextDependencyExtractor}).
     * Usually should be true, but recommended to disable if the change type corresponds to data migration code,
     * e.g. for the DB migration type in the DB implementation.
     */
    boolean isEnrichableForDependenciesInText();

    /**
     * Whether the object type for that implementation requires that its dependent object be recreated if this object
     * changes.
     *
     * @return
     */
    boolean isDependentObjectRecalculationRequired();

    /**
     * For rerunnable objects, returns the object type to use if a body is declared, e.g. for an Oracle package and its
     * corresponding package body.
     */
    ChangeType getBodyChangeType();
}
