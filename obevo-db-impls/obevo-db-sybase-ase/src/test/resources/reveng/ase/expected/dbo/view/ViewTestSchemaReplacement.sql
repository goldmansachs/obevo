CREATE VIEW ViewTestSchemaReplacement AS
SELECT a.*, b.*
from TestTable a
inner join TestTable2 b ON a.ID = b.FK
GO
