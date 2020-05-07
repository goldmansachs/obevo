//// METADATA DISABLE_QUOTED_IDENTIFIERS
create procedure ${dbdeploy01_subschemaSuffixed}ProcWithDoubleQuotes as select "abc", * from ${dbdeploy01_subschemaSuffixed}TestView
