//// CHANGE name=init
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
