-- functions are not supported in the in-memory DB platform translations; hence we limit this to DB2

CREATE OR REPLACE VIEW VIEW_DEPENDING_ON_FUNC AS SELECT FUNC_WITH_DEPENDENT_VIEW() MYVAL FROM TABLE_A
GO