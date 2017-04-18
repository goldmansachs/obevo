//// CHANGE name=change0
create table DefaultColTest (
	id                              int                              identity  ,
	default_str                     varchar(25)                     DEFAULT  '123' 
      null  ,
	default_nn_str                  varchar(25)                     DEFAULT  '123' 
  not null  ,
	default_curdate                 datetime                        DEFAULT  getdate() 
      null  ,
	default_date1                   datetime                        DEFAULT  '20 Jan 2005 12:12:12' 
      null  ,
	default_date2                   datetime                        DEFAULT  'Jan 20 2005' 
      null  ,
		CONSTRAINT PK_DefaultColTest PRIMARY KEY CLUSTERED ( id )  on 'default' 
)
lock datarows
 on 'default'
GO
