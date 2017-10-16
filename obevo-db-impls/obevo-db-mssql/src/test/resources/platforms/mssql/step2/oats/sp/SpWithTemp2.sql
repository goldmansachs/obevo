--
-- Copyright 2017 Goldman Sachs.
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

/* This is a use case from Costar */
create table #MyTemp (
    FieldA varchar(32),
    FieldB int
)
GO
create proc ${oats_subschemaSuffixed}SpWithTemp2 (@MaxCount  int) as
--$Header: /home/cvs/gdtech/costar/src/db/procs/DelExcessSelectTrades.eqd,v 1.2 2010/08/13 18:15:51 jayava Exp $
begin
    select * from #MyTemp
end
GO
drop table #MyTemp /* cleanup */
GO
