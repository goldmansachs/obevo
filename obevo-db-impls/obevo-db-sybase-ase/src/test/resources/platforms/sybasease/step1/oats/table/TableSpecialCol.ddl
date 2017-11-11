//// CHANGE name=init excludeEnvs="test%schema"
CREATE TABLE TableSpecialCol
(
	idField INT NOT NULL,
	smallDateTimeField smalldatetime,
	textField TEXT,
	booleanCol Boolean,
	boolean2Col Boolean2,
	univarfield univarchar(10) null,
	PRIMARY KEY (idField)
)
GO

//// CHANGE name=init includeEnvs="test%schema"
CREATE TABLE ${oats_subschemaSuffixed}TableSpecialCol
(
	idField INT NOT NULL,
	smallDateTimeField smalldatetime,
	textField TEXT,
	univarfield univarchar(10) null,
	PRIMARY KEY (idField)
)
GO
