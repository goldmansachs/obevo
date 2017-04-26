//// CHANGE name=chng1
CREATE TABLE TABLE_A (
	A_ID    INT	NOT NULL
)
GO

//// CHANGE name=ind1 changeset=phaseA
CREATE INDEX TABLE_A_IND1 ON TABLE_A(A_ID)
GO
