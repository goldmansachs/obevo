//// METADATA DISABLE_QUOTED_IDENTIFIERS
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
	CONSTRAINT PK PRIMARY KEY (idField)
) LOCK DATAROWS
GO

//// CHANGE name=bindings excludeEnvs=test%schema
-- excluding these from schema-based envs as these object types are not supported
sp_bindefault 'DateDefault', 'TestTable.stringDateField'
GO
sp_bindrule booleanRule, 'TestTable.myBooleanCol'
GO

//// CHANGE name=indexes excludeEnvs=test%schema
CREATE INDEX IND1 ON TestTable(stringField)
GO
DROP INDEX TestTable.IND1
GO
CREATE INDEX IND1 ON TestTable(stringField)
GO

//// CHANGE name=indexes includeEnvs=test%schema comment="dropIndexDoesntWorkInSchemaTable"
CREATE INDEX IND1 ON ${oats_subschemaSuffixed}TestTable(stringField)
GO

//// CHANGE TRIGGER name=trigger1
create trigger ${oats_subschemaSuffixed}TestTableTrigger1
on ${oats_subschemaSuffixed}TestTable
for insert
as
print "Added!"
