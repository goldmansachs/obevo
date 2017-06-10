//// METADATA DISABLE_QUOTED_IDENTIFIERS
-- ensure that ? is not interpreted as a parameter
create view TestView AS select idField "MyId", stringField "MyString", myBooleanCol "MyMyBooleanCol" from TestTable
GO