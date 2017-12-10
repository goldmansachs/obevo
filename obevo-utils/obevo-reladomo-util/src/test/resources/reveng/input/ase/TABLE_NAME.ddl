if exists (select * from sysobjects where name = 'TABLE_NAME' and type='U')
drop table TABLE_NAME
GO

create table TABLE_NAME
(
ID bigint not null,
TABLE_NAME_3 bigint not null,
TABLE_NAME_2 bigint not null,
OUT_UTC datetime not null
)
GO
