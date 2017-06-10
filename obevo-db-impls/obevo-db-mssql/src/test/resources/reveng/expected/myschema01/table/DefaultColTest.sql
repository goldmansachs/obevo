//// CHANGE name=change0
CREATE TABLE DefaultColTest(
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

//// CHANGE name=change1
ALTER TABLE DefaultColTest ADD  DEFAULT ('123') FOR [default_str]
GO

//// CHANGE name=change2
ALTER TABLE DefaultColTest ADD  DEFAULT ('123') FOR [default_nn_str]
GO

//// CHANGE name=change3
ALTER TABLE DefaultColTest ADD  DEFAULT (getdate()) FOR [default_curdate]
GO

//// CHANGE name=change4
ALTER TABLE DefaultColTest ADD  DEFAULT ('20 Jan 2005 12:12:12') FOR [default_date1]
GO

//// CHANGE name=change5
ALTER TABLE DefaultColTest ADD  DEFAULT ('Jan 20 2005') FOR [default_date2]
GO
