/****** Object:  Table [dbo].[TableSpecialCol]    Script Date: 1/1/2017 12:00:00 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TableSpecialCol](
	[idField] [int] NOT NULL,
	[smallDateTimeField] [smalldatetime] NULL,
	[textField] [text] COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	[booleanCol] [dbo].[Boolean] NOT NULL,
	[boolean2Col] [dbo].[Boolean2] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[idField] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
EXEC sys.sp_bindrule @rulename=N'[dbo].[booleanRule]', @objname=N'[dbo].[TableSpecialCol].[booleanCol]' , @futureonly='futureonly'
GO
EXEC sys.sp_bindrule @rulename=N'[dbo].[booleanRule2]', @objname=N'[dbo].[TableSpecialCol].[boolean2Col]' , @futureonly='futureonly'
GO
