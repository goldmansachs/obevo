//// METADATA DISABLE_QUOTED_IDENTIFIERS
create procedure ProcWithDoubleQuotes as/* Adaptive Server has expanded all '*' elements in the following statement */  select "abc", TestView.MyId, TestView.MyString, TestView.MyMyBooleanCol from TestView
GO
sp_procxmode 'ProcWithDoubleQuotes', unchained
GO