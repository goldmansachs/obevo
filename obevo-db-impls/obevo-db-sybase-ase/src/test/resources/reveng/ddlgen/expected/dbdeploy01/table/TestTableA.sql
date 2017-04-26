//// CHANGE name=change0
create table TestTableA (
	id                              int                              not null  ,
	tId                             int                                  null  ,
	test                            varchar(25)                          null  ,
	fk                              int                              not null  ,
		CONSTRAINT PK PRIMARY KEY CLUSTERED ( id )  on 'default' 
)
lock datarows
 on 'default'
GO

//// CHANGE INDEX name=IND1
create nonclustered index IND1 
on TestTableA(test)
GO

//// CHANGE FK name=TestTableA_408385493
alter table TestTableA
add constraint TestTableA_408385493 FOREIGN KEY (fk) REFERENCES TestTable(idField)
GO
