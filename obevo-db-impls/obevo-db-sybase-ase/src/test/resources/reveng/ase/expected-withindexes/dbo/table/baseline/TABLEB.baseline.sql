CREATE TABLE TABLEB (
    col1    varchar(14) NOT NULL,
    col2    varchar(10) NOT NULL,
    col3    varchar(20) NOT NULL,
    col4    char(2) NOT NULL,
    col5    datetime NOT NULL,
    col6    float NULL
)
LOCK ALLPAGES
GO
CREATE UNIQUE CLUSTERED INDEX idx2
    ON TABLEB(col1, col2, col3)
GO
CREATE UNIQUE NONCLUSTERED INDEX idx4
    ON TABLEB(col4, col5)
GO