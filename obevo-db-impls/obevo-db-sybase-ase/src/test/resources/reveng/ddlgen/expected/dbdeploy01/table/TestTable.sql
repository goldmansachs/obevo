//// CHANGE name=change0
create table TestTable (
	idField                         int                              not null  ,
	stringField                     varchar(100)                     not null  ,
	stringDateField                 date                                  null  ,
	dateTimeField                   datetime                             null  ,
	myBooleanCol                    int                                  null   ,
	tinyIntCol                      tinyint                          not null  ,
	timeUpdated                     datetime                         not null  ,
	textField                       text                                 null  ,
	myNewCol                        int                                  null  ,
		CONSTRAINT PK PRIMARY KEY CLUSTERED ( idField )  on 'default' 
)
lock datarows
 on 'default'
GO

//// CHANGE name=change1
sp_bindefault 'DateDefault', 'TestTable.stringDateField'
GO

//// CHANGE name=change2
sp_bindrule 'booleanRule', 'TestTable.myBooleanCol'
GO

//// CHANGE INDEX name=IND1
create nonclustered index IND1 
on TestTable(stringField)
GO
