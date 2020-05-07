//// CHANGE name=init
CREATE TABLE ${dbdeploy01_subschemaSuffixed}TableSpecialCol
(
	idField INT NOT NULL,
	smallDateTimeField smalldatetime,
	textField TEXT,
	booleanCol ${dbdeploy01_subschemaSuffixed}Boolean,
	boolean2Col ${dbdeploy01_subschemaSuffixed}Boolean2,
	PRIMARY KEY (idField)
)
GO
