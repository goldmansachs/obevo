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

//// CHANGE name=base
CREATE TABLE SECURITYUNDERLYER  (
	ID         	BIGINT auto_increment NOT NULL,
	HIBLEGID   	BIGINT,
	HIBRISKID  	BIGINT,
	LEGID      	VARCHAR(42),
	UNDERLYERID	VARCHAR(32),
	PRIMEID    	INTEGER,
	GSN        	VARCHAR(7),
	UNITS      	DOUBLE,
	ENTITYID   	INTEGER,
	RIC        	VARCHAR(20),
	CUSIP      	VARCHAR(9),
	ENTITYNAME 	VARCHAR(255),
	DEALID     	VARCHAR(50),
	CONSTRAINT SQL060823061150910 PRIMARY KEY(ID)

 )
GO
CREATE INDEX UNDERLYER_INDEX1
	ON SECURITYUNDERLYER(HIBLEGID)
GO
CREATE INDEX UNDERLYER_INDEX2
	ON SECURITYUNDERLYER(UNDERLYERID)
GO
CREATE INDEX UNDERLYER_INDEX3
	ON SECURITYUNDERLYER(PRIMEID, GSN)
GO
CREATE INDEX UNDERLYER_INDEX5
	ON SECURITYUNDERLYER(HIBRISKID)
GO