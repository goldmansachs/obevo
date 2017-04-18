CREATE VIEW ViewTestSchemaReplacement AS
SELECT a.*, b.*
from dbdeploy01.dbo.TestTable a
inner join dbdeploy01.dbo.TestTable2 b ON a.ID = b.FK
GO
