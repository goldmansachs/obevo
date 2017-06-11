CREATE FUNCTION func1() RETURNS integer
    LANGUAGE plpgsql
    AS '
BEGIN
    -- ensure that func comment remains
    RETURN 1;
END;
';



GO