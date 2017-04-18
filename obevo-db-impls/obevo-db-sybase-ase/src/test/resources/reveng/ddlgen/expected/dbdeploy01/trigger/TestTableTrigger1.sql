//// METADATA DISABLE_QUOTED_IDENTIFIERS
create trigger TestTableTrigger1
on TestTable
for insert
as
print "Added!"
GO