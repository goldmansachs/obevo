/****** Object:  View [dbo].[TestView]    Script Date: 1/1/2017 12:00:00 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER OFF
GO
-- ensure that ? is not interpreted as a parameter
create view TestView AS select idField "MyId", stringField "MyString", myBooleanCol "MyMyBooleanCol" from TestTable
GO
