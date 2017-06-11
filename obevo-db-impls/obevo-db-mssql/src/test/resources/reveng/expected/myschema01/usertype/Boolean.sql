CREATE TYPE Boolean FROM [tinyint] NOT NULL
GO
EXEC sys.sp_bindrule @rulename=N'booleanRule', @objname=N'Boolean' , @futureonly='futureonly'
GO