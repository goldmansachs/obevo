//// CHANGE name="init" comment="testing table with comments only on columns"
CREATE TABLE COMMENT_COL_TABLE
(
    ID NUMBER NOT NULL,
    VAL2 NUMBER NULL
)
GO

//// CHANGE name="init1"
COMMENT ON COLUMN COMMENT_COL_TABLE.ID
IS 'comment col table id'
GO

//// CHANGE name="init2"
COMMENT ON COLUMN COMMENT_COL_TABLE.VAL2
IS 'comment col table val2'
GO
