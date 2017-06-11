//// CHANGE name=change0
CREATE TABLE TableSpecialCol(
	[idField] [int] NOT NULL,
	[smallDateTimeField] [smalldatetime] NULL,
	[textField] [text] COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	[booleanCol] Boolean NOT NULL,
	[boolean2Col] Boolean2 NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[idField] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO

//// CHANGE name=change1
EXEC sys.sp_bindrule @rulename=N'booleanRule', @objname=N'TableSpecialCol.[booleanCol]' , @futureonly='futureonly'
GO

//// CHANGE name=change2
EXEC sys.sp_bindrule @rulename=N'booleanRule2', @objname=N'TableSpecialCol.[boolean2Col]' , @futureonly='futureonly'
GO
