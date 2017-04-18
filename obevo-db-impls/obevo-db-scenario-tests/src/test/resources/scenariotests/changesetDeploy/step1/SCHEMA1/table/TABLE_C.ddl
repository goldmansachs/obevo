//// CHANGE name=chng1
CREATE TABLE TABLE_C (
	C_ID    BIGINT NOT NULL
)


//// CHANGE name=ind1 changeset=phaseB
CREATE INDEX TABLE_C_IND1 ON TABLE_C(C_ID)
GO
