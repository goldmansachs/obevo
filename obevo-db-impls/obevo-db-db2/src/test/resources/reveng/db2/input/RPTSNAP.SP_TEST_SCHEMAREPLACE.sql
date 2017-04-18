CREATE PROCEDURE SP_TEST_SCHEMAREPLACE(IN ARG INTEGER)
    LANGUAGE SQL
BEGIN
    DELETE FROM RPTSNAP.COSProcStatus;  -- replacing this schema prefix to remove the qualifier
    DELETE FROM OTHERSCHEMA.COSProcStatus;  -- not replacing this schema prefix as it is in a different schema
END
GO
