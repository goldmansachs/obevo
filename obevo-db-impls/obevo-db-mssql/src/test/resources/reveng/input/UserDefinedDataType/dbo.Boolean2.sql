/****** Object:  UserDefinedDataType [dbo].[Boolean2]    Script Date: 1/1/2017 12:00:00 AM ******/
CREATE TYPE [dbo].[Boolean2] FROM [tinyint] NOT NULL
GO
EXEC sys.sp_bindrule @rulename=N'[dbo].[booleanRule2]', @objname=N'[dbo].[Boolean2]' , @futureonly='futureonly'
GO
