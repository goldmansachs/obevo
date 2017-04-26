//// METADATA excludeDependencies="SP2"
CREATE PROCEDURE SP3 (IN INVAL INT)
/* adding text for SP2 to validate that we can override dependencies */
BEGIN ATOMIC
    insert into TABLE_A (A_ID, B_ID) values (INVAL, 5);
END
GO

//// DROP_COMMAND
DROP PROCEDURE SP3(INT)
