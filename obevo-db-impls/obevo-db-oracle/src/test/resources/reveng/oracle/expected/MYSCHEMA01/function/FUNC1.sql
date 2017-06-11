CREATE OR REPLACE EDITIONABLE FUNCTION FUNC1 
RETURN integer IS
BEGIN
    -- ensure that func comment remains
    RETURN 1;
END;

GO