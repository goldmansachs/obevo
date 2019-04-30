CREATE OR REPLACE EDITIONABLE TRIGGER ${schema1_physicalName}.TRIGGER1
AFTER LOGON
ON ${schema1_physicalName}.schema
--ON database
DECLARE
BEGIN
EXECUTE IMMEDIATE 'ALTER SESSION SET CURRENT_SCHEMA=${schema1_physicalName}';
END
/
ALTER TRIGGER ${schema1_physicalName}.TRIGGER1 ENABLE
