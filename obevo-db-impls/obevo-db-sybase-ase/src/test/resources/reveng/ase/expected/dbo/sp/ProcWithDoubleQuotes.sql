//// METADATA DISABLE_QUOTED_IDENTIFIERS
CREATE PROCEDURE ProcWithDoubleQuotes as select "abc", * from TestView
GO
sp_procxmode 'ProcWithDoubleQuotes', 'Unchained'
GO