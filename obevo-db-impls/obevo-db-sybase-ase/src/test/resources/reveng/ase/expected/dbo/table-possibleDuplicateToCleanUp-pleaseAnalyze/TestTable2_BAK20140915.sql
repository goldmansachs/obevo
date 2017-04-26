//// CHANGE name=init
// creating this file to simulate the backups that people have taken in production
CREATE TABLE TestTable2 (
	TID 	int NULL,
	test	varchar(25) NULL,
	FK  	int NOT NULL 
	)
GO

//// CHANGE FK name=initFk
ALTER TABLE TestTable2
	ADD CONSTRAINT TEST_TABLE_1205680412
	FOREIGN KEY(FK)
	REFERENCES TestTable(ID)
GO
