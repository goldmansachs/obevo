/****** Object:  UserDefinedDataType [dbo].[Boolean]    Script Date: 1/1/2017 12:00:00 AM ******/
CREATE TYPE [dbo].[Boolean] FROM [tinyint] NOT NULL
GO
EXEC sys.sp_bindrule @rulename=N'[dbo].[booleanRule]', @objname=N'[dbo].[Boolean]' , @futureonly='futureonly'
GO
