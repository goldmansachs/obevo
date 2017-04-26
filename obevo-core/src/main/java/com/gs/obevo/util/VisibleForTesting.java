/*
 * Copyright (C) 2006 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Borrowed from <a href="https://google.github.io/guava/releases/21.0/api/docs/com/google/common/annotations/VisibleForTesting.html">Google Guava</a>.
 * We code this here directly to avoid the heavier Guava dependency.
 */
package com.gs.obevo.util;

/**
 * An annotation that indicates that the visibility of a type or member has
 * been relaxed to make the code testable.
 *
 * @author Johannes Henkel
 */
public @interface VisibleForTesting {
}
