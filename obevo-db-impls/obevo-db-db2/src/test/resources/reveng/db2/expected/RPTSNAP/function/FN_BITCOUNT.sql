CREATE FUNCTION FN_BITCOUNT (N1 Integer)
 RETURNS integer
 SPECIFIC APPDIR.BIT_COUNT
 LANGUAGE SQL
 DETERMINISTIC
 NO EXTERNAL ACTION
 CONTAINS SQL
BEGIN ATOMIC
DECLARE M1, i, len  Integer default 0;
DECLARE  bit_count integer default 0;

  SET M1 = N1;
  WHILE  M1 > 0  DO
   if(mod(m1,2) = 1) then
    set bit_count = bit_count + 1;
   end if;
   set m1 = m1/2;
  END WHILE;

RETURN bit_count;
END

GO