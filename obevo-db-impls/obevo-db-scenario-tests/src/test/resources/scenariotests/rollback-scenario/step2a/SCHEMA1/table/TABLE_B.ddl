//// CHANGE name=chng1
CREATE TABLE TABLE_B (
	B_ID    INT	NOT NULL,
    PRIMARY KEY (B_ID)
)

//// CHANGE name=ADD_COLABC_toRollBackImmediately
ALTER TABLE TABLE_B ADD COLUMN COLABC INT NULL
GO
// ROLLBACK-IF-ALREADY-DEPLOYED
ALTER TABLE TABLE_B DROP COLUMN COLABC
GO

//// CHANGE name=ADD_COL2
ALTER TABLE TABLE_B ADD COLUMN COL2 INT NULL
GO
// ROLLBACK
garbageRollback - will be corrected in step2b

-- note - to test the proper rollback order, this changed script should come before the next one for colindex. As even if we update this rollback script later on, the rollback execution should respect the original change "insertion" order

//// CHANGE name=INDEX_TABLE_B_COL2_IND
CREATE INDEX TABLE_B_COL2_IND ON TABLE_B(COL2)
GO
// ROLLBACK
DROP INDEX TABLE_B_COL2_IND
GO
