CREATE FUNCTION func_with_overload() RETURNS integer
    LANGUAGE plpgsql
    AS '
BEGIN
    RETURN 1;
END;
';



GO
CREATE FUNCTION func_with_overload(var1 integer) RETURNS integer
    LANGUAGE plpgsql
    AS '
BEGIN
    RETURN 1;
END;
';



GO
CREATE FUNCTION func_with_overload(var1 integer, invalstr character varying) RETURNS integer
    LANGUAGE plpgsql
    AS '
BEGIN
    RETURN 1;
END;
';





GO