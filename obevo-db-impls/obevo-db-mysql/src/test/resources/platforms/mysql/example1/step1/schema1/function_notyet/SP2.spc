CREATE FUNCTION SP2 (IN myinput INT, OUT mycount INT) AS $$
BEGIN
    SELECT count(*) into mycount FROM TABLE_A;
END;
$$ LANGUAGE plpgsql;
GO
