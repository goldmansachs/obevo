CREATE TABLE TABLEA (
    ApplicationName varchar(64) NOT NULL,
    BuildNumber int NOT NULL
    )
LOCK ALLPAGES
GO
CREATE UNIQUE CLUSTERED INDEX idx1
    ON TABLEA(ApplicationName)
GO