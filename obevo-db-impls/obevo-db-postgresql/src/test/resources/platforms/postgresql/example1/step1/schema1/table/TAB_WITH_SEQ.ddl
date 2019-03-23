//// METADATA excludePlatforms=REDSHIFT
//// CHANGE name=chng1
CREATE TABLE TAB_WITH_SEQ (
	ID           	int4 NOT NULL DEFAULT nextval('MYSEQ1'::regclass),
	FIELD1    INT	NULL,
    PRIMARY KEY (ID)
) WITHOUT OIDS
GO
