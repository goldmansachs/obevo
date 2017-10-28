//// CHANGE name=init
CREATE TABLE ${oats_subschemaSuffixed}TableSpecialCol
(
	idField INT NOT NULL,
	smallDateTimeField smalldatetime,
	textField TEXT,
	booleanCol ${oats_subschemaSuffixed}Boolean,
	boolean2Col ${oats_subschemaSuffixed}Boolean2,
	PRIMARY KEY (idField)
)
GO
