//// CHANGE name=chng1
CREATE TABLE TABLE_A (
	A_ID    INT	NOT NULL,
	A_VAL   VARCHAR(32) NULL,
	B_ID INT NULL
)
GO

//// CHANGE name=chng2
ALTER TABLE TABLE_A ADD COLUMN A_VAL2 VARCHAR(32)
GO

//// CHANGE name=chng3 dependencies="migration2.chng2,migration1.chng3"
ALTER TABLE TABLE_A DROP COLUMN A_VAL
GO
