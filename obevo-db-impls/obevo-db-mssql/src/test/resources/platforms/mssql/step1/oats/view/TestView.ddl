//// METADATA DISABLE_QUOTED_IDENTIFIERS
-- ensure that ? is not interpreted as a parameter
create view ${oats_subschemaSuffixed}TestView AS select idField "MyId", stringField "MyString", myBooleanCol "MyMyBooleanCol" from ${oats_subschemaSuffixed}TestTable
