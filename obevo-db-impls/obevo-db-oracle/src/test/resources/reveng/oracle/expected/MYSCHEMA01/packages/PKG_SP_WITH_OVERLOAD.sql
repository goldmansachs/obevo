CREATE OR REPLACE EDITIONABLE PACKAGE PKG_SP_WITH_OVERLOAD 
AS
    PROCEDURE SP_WITH_OVERLOAD;
    PROCEDURE SP_WITH_OVERLOAD(INVAL IN integer);
    PROCEDURE SP_WITH_OVERLOAD(var1 IN integer, INVALSTR IN VARCHAR);
END;


CREATE OR REPLACE EDITIONABLE PACKAGE BODY PKG_SP_WITH_OVERLOAD 
AS
    PROCEDURE SP_WITH_OVERLOAD IS
    BEGIN
        DELETE FROM TABLE_A;
    END;

    PROCEDURE SP_WITH_OVERLOAD (INVAL IN integer) IS
    BEGIN
        DELETE FROM TABLE_A;
    END;

    PROCEDURE SP_WITH_OVERLOAD (var1 IN integer, INVALSTR IN VARCHAR) IS
    BEGIN
        DELETE FROM TABLE_A;
    END;
END;

GO