CREATE PROCEDURE SP2 (OUT mycount INT)
READS SQL DATA
BEGIN ATOMIC
    DECLARE myvar INT;
    SELECT count(*) into mycount FROM TABLE_A;
    SELECT count(*) into mycount FROM TABLE_B;
    CALL SP1(myvar);
    CALL SP3(myvar);
END
GO
