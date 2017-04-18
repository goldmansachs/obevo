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
/**
 * This package defines the commands and interfaces that can be applied against an {@link com.gs.obevo.api.appdata.Environment}.
 *
 * The full set of incremental deployment logic should work based on the classes here, e.g. in terms of when to deploy
 * vs undeploy/rollback, whether there are any hash mismatch issues, ...
 *
 * In terms of "how" the deployments are applied to the environments, that is elsewhere (e.g. {@link com.gs.obevo.api.platform.ChangeType}).
 */
package com.gs.obevo.impl.command;