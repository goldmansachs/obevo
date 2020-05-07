//// METADATA DISABLE_QUOTED_IDENTIFIERS
-- ensure that ? is not interpreted as a parameter
create view ${dbdeploy01_subschemaSuffixed}TestView AS select idField "MyId", stringField "MyString", myBooleanCol "MyMyBooleanCol" from ${dbdeploy01_subschemaSuffixed}TestTable
