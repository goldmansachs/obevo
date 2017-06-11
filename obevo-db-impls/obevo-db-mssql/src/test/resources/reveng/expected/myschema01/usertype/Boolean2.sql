CREATE TYPE Boolean2 FROM [tinyint] NOT NULL
GO
EXEC sys.sp_bindrule @rulename=N'booleanRule2', @objname=N'Boolean2' , @futureonly='futureonly'
GO