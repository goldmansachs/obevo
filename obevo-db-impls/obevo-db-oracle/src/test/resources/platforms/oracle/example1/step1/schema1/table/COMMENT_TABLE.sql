//// CHANGE name="init"
CREATE TABLE COMMENT_TABLE
(
    ID NUMBER NOT NULL
)
GO

//// CHANGE name="init1"
COMMENT ON TABLE COMMENT_TABLE
IS 'comment1'
GO

//// CHANGE name="init2"
COMMENT ON COLUMN COMMENT_TABLE.ID
IS 'comment2'
GO
