//// CHANGE name=change0
CREATE TABLE TestTable(
	[idField] [int] NOT NULL,
	[stringField] [varchar](100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	[stringDateField] [date] NULL,
	[dateTimeField] [datetime] NULL,
	[myBooleanCol] [int] NULL,
	[tinyIntCol] [tinyint] NOT NULL,
	[timeUpdated] [datetime] NOT NULL,
	[textField] [text] COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	[myNewCol] [int] NULL,
 CONSTRAINT [TestTable_PK] PRIMARY KEY CLUSTERED 
(
	[idField] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO

//// CHANGE name=change1
EXEC sys.sp_bindefault @defname=N'DateDefault', @objname=N'TestTable.[stringDateField]' , @futureonly='futureonly'
GO

//// CHANGE name=change2
EXEC sys.sp_bindrule @rulename=N'booleanRule', @objname=N'TestTable.[myBooleanCol]' , @futureonly='futureonly'
GO

//// CHANGE INDEX name=IND1
CREATE NONCLUSTERED INDEX [IND1] ON TestTable
(
	[stringField] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO

//// CHANGE name=change3
ALTER TABLE TestTable ENABLE TRIGGER [TestTableTrigger1]
GO
