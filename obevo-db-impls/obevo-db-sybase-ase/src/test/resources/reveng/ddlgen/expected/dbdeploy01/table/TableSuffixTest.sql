//// CHANGE name=change0
create table TableSuffixTest (
	idField                         int                              not null  ,
 PRIMARY KEY CLUSTERED ( idField )  on 'default' 
)
lock datarows
with exp_row_size = 1 on 'default'
GO
