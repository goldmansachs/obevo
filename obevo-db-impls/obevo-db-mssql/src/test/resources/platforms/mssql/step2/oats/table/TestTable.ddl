//// CHANGE name=init
CREATE TABLE ${oats_subschemaSuffixed}TestTable
(
	idField INT NOT NULL,
	stringField VARCHAR(100),
	stringDateField DATE NULL,
	dateTimeField DATETIME NULL,
	myBooleanCol INT NULL,
	tinyIntCol tinyint NOT NULL,
	timeUpdated DATETIME NOT NULL,
	textField TEXT NULL,
	CONSTRAINT TestTable_PK PRIMARY KEY (idField)
)
GO

//// CHANGE name=bindings excludeEnvs=test%schema
-- excluding these from schema-based envs as these object types are not supported
sp_bindefault 'DateDefault', 'TestTable.stringDateField'
GO
sp_bindrule booleanRule, 'TestTable.myBooleanCol'
GO

//// CHANGE name=indexes
CREATE INDEX IND1 ON ${oats_subschemaSuffixed}TestTable(stringField)
GO
DROP INDEX ${oats_subschemaSuffixed}TestTable.IND1
GO
CREATE INDEX IND1 ON ${oats_subschemaSuffixed}TestTable(stringField)
GO

//// CHANGE name=modify
ALTER TABLE ${oats_subschemaSuffixed}TestTable ADD myNewCol2 INT NULL
GO

//// CHANGE name=rename excludeEnvs="unittest*"
sp_rename '${oats_subschemaSuffixed}TestTable.myNewCol2', 'myNewCol'
GO

//// CHANGE name=rename includeEnvs="unittest*"
ALTER TABLE ${oats_subschemaSuffixed}TestTable ALTER COLUMN myNewCol2 RENAME TO myNewCol
GO
