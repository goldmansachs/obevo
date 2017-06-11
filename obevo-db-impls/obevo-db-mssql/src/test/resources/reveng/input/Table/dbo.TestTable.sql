/****** Object:  Table [dbo].[TestTable]    Script Date: 1/1/2017 12:00:00 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TestTable](
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
EXEC sys.sp_bindefault @defname=N'[dbo].[DateDefault]', @objname=N'[dbo].[TestTable].[stringDateField]' , @futureonly='futureonly'
GO
EXEC sys.sp_bindrule @rulename=N'[dbo].[booleanRule]', @objname=N'[dbo].[TestTable].[myBooleanCol]' , @futureonly='futureonly'
GO
SET ANSI_PADDING ON

GO
/****** Object:  Index [IND1]    Script Date: 1/1/2017 12:00:00 AM ******/
CREATE NONCLUSTERED INDEX [IND1] ON [dbo].[TestTable]
(
	[stringField] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
/****** Object:  Trigger [dbo].[TestTableTrigger1]    Script Date: 1/1/2017 12:00:00 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER OFF
GO
create trigger TestTableTrigger1
on TestTable
for insert
as
print "Added!"
GO
ALTER TABLE [dbo].[TestTable] ENABLE TRIGGER [TestTableTrigger1]
GO
