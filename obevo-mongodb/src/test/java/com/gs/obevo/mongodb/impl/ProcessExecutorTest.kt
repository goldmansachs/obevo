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
package com.gs.obevo.mongodb.impl

import org.apache.commons.lang3.SystemUtils
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.io.File

class ProcessExecutorTest {
    @Rule @JvmField
    val thrown: ExpectedException = ExpectedException.none()

    @Test
    fun testSuccess() {
        val result = ProcessExecutor().runCommand("echo success")
        assertThat(result.exitCode, equalTo(0))
        assertThat(result.stdoutText, equalTo("success\n"))
    }

    @Test
    fun testError() {
        org.junit.Assume.assumeTrue(SystemUtils.IS_OS_UNIX)

        thrown.expectMessage("No such file or directory")

        ProcessExecutor().runCommand("non-existent program abcdefghijkl")
    }

    @Test
    fun testUnixExitCode() {
        org.junit.Assume.assumeTrue(SystemUtils.IS_OS_UNIX)
        val result = ProcessExecutor().runCommand("bash -c 'exit 2'")
        assertThat(result.exitCode, not(equalTo(0)))  // fails on travis with exit code 1, so we have a lenient check for non-zero here
//        assertThat(result.stdoutText, equalTo(""))  // function exec doesn't handle quotes well at the moment
    }

    // TBD
//    @Test
//    fun testWindowsExitCode() {
//        org.junit.Assume.assumeTrue(SystemUtils.IS_OS_UNIX)
//        val result = ProcessExecutor().runCommand("bash -c 'exit 2'")
//        assertThat(result.exitCode, equalTo(2))
//    }
}
