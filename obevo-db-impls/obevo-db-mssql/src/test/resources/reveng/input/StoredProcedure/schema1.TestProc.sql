/****** Object:  StoredProcedure [schema1].[TestProc]    Script Date: 1/1/2017 12:00:00 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
create procedure schema1.TestProc as select * from schema1.TestView
GO
/****** Object:  NumberedStoredProcedure [schema1].[TestProc];2    Script Date: 1/1/2017 12:00:00 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
create procedure schema1.TestProc;2 (@param1  int) as select * from schema1.TestView
GO
