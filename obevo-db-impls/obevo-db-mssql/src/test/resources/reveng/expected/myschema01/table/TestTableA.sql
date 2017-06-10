//// CHANGE name=change0
CREATE TABLE [dbo].[TestTableA](
	[id] [int] NOT NULL,
	[tId] [int] NULL,
	[test] [varchar](25) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	[fk] [int] NOT NULL,
 CONSTRAINT [TestTableA_PK] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO

//// CHANGE name=change1
SET ANSI_PADDING ON

GO

//// CHANGE INDEX name=IND1
CREATE NONCLUSTERED INDEX [IND1] ON [dbo].[TestTableA]
(
	[test] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO

//// CHANGE name=change2
ALTER TABLE [dbo].[TestTableA]  WITH CHECK ADD FOREIGN KEY([fk])
REFERENCES [dbo].[TestTable] ([idField])
GO

//// CHANGE name=change3
CREATE TYPE [dbo].[Boolean] FROM [tinyint] NOT NULL
GO

//// CHANGE name=change4
EXEC sys.sp_bindrule @rulename=N'[dbo].[booleanRule]', @objname=N'[dbo].[Boolean]' , @futureonly='futureonly'
GO

//// CHANGE name=change5
CREATE TYPE [dbo].[Boolean2] FROM [tinyint] NOT NULL
GO

//// CHANGE name=change6
EXEC sys.sp_bindrule @rulename=N'[dbo].[booleanRule2]', @objname=N'[dbo].[Boolean2]' , @futureonly='futureonly'
GO
