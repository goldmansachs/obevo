//// CHANGE name=change0
create table TableSpecialCol (
	idField                         int                              not null  ,
	smallDateTimeField              smalldatetime                    not null  ,
	textField                       text                             not null  ,
	booleanCol                      Boolean                          not null   ,
	boolean2Col                     Boolean2                         not null   ,
 PRIMARY KEY CLUSTERED ( idField )  on 'default' 
)
lock allpages
 on 'default'
GO

//// CHANGE name=change1
sp_bindrule 'booleanRule', 'TableSpecialCol.booleanCol'
GO

//// CHANGE name=change2
sp_bindrule 'booleanRule2', 'TableSpecialCol.boolean2Col'
GO
