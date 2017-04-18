CREATE TABLE TestTable (
	ID               	int NOT NULL,
	STRING           	varchar(100) NOT NULL,
	STRING_DATE_FIELD	date NULL,
	MY_BOOLEAN_COL   	int NULL,
	MYNEWCOL         	int NULL,
	PRIMARY KEY(ID)
	WITH max_rows_per_page = 0
	)
GO
sp_bindefault 'DateDefault', 'dbo.TEST_TABLE.STRING_DATE_FIELD'
GO
create trigger TEST_TABLE_TRIGGER1
on TestTable
for insert
as
print 'Added!'
GO
create trigger TEST_TABLE_TRIGGER2
on TestTable
for insert
as
print 'Added!'
GO
CREATE TRIGGER TEST_TABLE_TRIGGER3
on TestTable
for insert
as
print 'Added!'
GO