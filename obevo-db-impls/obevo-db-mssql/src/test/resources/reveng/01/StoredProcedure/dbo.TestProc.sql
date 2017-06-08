/****** Object:  StoredProcedure [dbo].[TestProc]    Script Date: 1/1/2017 12:00:00 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
create procedure TestProc as select * from TestView
GO
/****** Object:  NumberedStoredProcedure [dbo].[TestProc];2    Script Date: 1/1/2017 12:00:00 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
create procedure TestProc;2 (@param1  int) as select * from TestView
GO
