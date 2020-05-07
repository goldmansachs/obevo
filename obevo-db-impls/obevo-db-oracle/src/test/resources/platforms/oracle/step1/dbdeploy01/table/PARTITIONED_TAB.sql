//// CHANGE name=init includeEnvs="noEnvsYet" comment="Want to test INDEX PARTITION clause in reverse engineering, but Docker doesn't support partitioning - ORA-00439: feature not enabled: Partitioning"
CREATE TABLE PARTITIONED_TAB (
	A_ID    INT	NOT NULL,
	B_ID    INT	NOT NULL
)
GO

//// CHANGE name=PARTITIONED_TAB_PART
CREATE INDEX PARTITIONED_TAB_PART
 ON PARTITIONED_TAB(B_ID)
 LOCAL PARALLEL NOLOGGING
GO