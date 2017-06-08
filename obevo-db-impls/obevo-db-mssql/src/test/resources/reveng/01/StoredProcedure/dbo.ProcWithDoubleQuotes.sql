/****** Object:  StoredProcedure [dbo].[ProcWithDoubleQuotes]    Script Date: 1/1/2017 12:00:00 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER OFF
GO
create procedure ProcWithDoubleQuotes as select "abc", * from TestView
GO
