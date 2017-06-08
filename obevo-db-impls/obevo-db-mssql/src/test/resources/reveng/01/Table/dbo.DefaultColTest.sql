/****** Object:  Table [dbo].[DefaultColTest]    Script Date: 1/1/2017 12:00:00 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[DefaultColTest](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[default_str] [varchar](25) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
	[default_nn_str] [varchar](25) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
	[default_curdate] [datetime] NULL,
	[default_date1] [datetime] NULL,
	[default_date2] [datetime] NULL,
 CONSTRAINT [PK_DefaultColTest] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
ALTER TABLE [dbo].[DefaultColTest] ADD  DEFAULT ('123') FOR [default_str]
GO
ALTER TABLE [dbo].[DefaultColTest] ADD  DEFAULT ('123') FOR [default_nn_str]
GO
ALTER TABLE [dbo].[DefaultColTest] ADD  DEFAULT (getdate()) FOR [default_curdate]
GO
ALTER TABLE [dbo].[DefaultColTest] ADD  DEFAULT ('20 Jan 2005 12:12:12') FOR [default_date1]
GO
ALTER TABLE [dbo].[DefaultColTest] ADD  DEFAULT ('Jan 20 2005') FOR [default_date2]
GO
