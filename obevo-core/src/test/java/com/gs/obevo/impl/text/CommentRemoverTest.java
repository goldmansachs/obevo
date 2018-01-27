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
package com.gs.obevo.impl.text;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class CommentRemoverTest {
    @Test
    public void testRemoveComments() {
        String stringWithoutComment = CommentRemover.removeComments("create procedure sp1\n" +
                "		        // Comment sp2\n" +
                "		        -- Comment sp2\n" +
                "                call sp_3(1234)  -- end of line comment sp5\n" +
                "		        /* Comment sp5 */\n" +
                "		        sp6 -- ensure that this line still remains (w/ the sp6) between the block comments\n" +
                "		        /*--------- Comment\n" +
                "		        sp5\n" +
                "Ensure that the end-block comment is still recognized, even though the line itself starts w/ a comment.\n" +
                "This is because this part is within a block\n" +
                "---------*/\n" +
                "				call sp4(1234)\n" +
                "\n" +
                "		///		/*\n" +
                "		This should not get commented out, as the previous 'start block' was itself commented out\n" +
                "				 */\n" +
                "\n" +
                "\n" +
                "				end\n" +
                "", "testlog");

        assertThat(stringWithoutComment, Matchers.equalToIgnoringWhiteSpace("create procedure sp1\n" +
                "                call sp_3(1234)\n" +
                "                sp6\n" +
                "                call sp4(1234)\n" +
                "                This should not get commented out, as the previous 'start block' was itself commented out\n" +
                "                */\n" +
                "                end"));
    }

    @Test
    public void testReturnGracefullyIfParsingFails() {
        String content = "abc \"def\" un-closed quote \" un-closed /* comment not removed */ quote";
        assertEquals(content, CommentRemover.removeComments(content, "testlog"));
    }

    @Test
    public void testHandleUnclosedQuoteWithinComments() {
        String content = "abc \"def\" '1' ghi /* comment ' removed */  '2' jkl";
        assertThat(CommentRemover.removeComments(content, "testlog"), Matchers.equalToIgnoringWhiteSpace(
                "abc \"def\" '1' ghi '2' jkl"
        ));
    }
}
