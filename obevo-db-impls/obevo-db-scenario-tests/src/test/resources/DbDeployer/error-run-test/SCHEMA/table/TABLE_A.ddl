//// CHANGE name=chng1
CREATE TABLE TABLE_A (
	A_ID    INT	NOT NULL,
	B_ID    INT	NOT NULL,
	STRING_FIELD	VARCHAR(30)	NULL,
    TIMESTAMP_FIELD	TIMESTAMP	NULL,
	DEFAULT_FIELD TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP, 
    PRIMARY KEY (A_ID)

)
GO

//// CHANGE name=invalidChange
fasdlkfjsdfasdfdsaffd
GO
//// CHANGE name=extra1
ALTER TABLE TABLE_A ADD COLUMN EXTRA1 INT NULL
GO
//// CHANGE FK name=failed_fk_not_to_be_attempted
this bad change should not get invoked as the earlier invalidChange failed
GO
