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
package com.gs.obevo.apps.reveng;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test that contains common logic for all reverse-engineer tools based on {@link AbstractReveng}.
 *
 * For now - we have no common logic but want to refactor in the future. This class is still useful as it facilitates
 * running all tests in one shot in the IDE.
 */
@Ignore("The child classes should get executed")
public abstract class AbstractRevengTest {
    @Test
    public abstract void testReverseEngineeringFromFile() throws Exception;
}
