//// CHANGE name=change0
create table TABLE_NAME
(
ID bigint not null,
TABLE_NAME_3 bigint not null,
TABLE_NAME_2 bigint not null,
OUT_UTC datetime not null
)

GO

//// CHANGE INDEX name=TABLE_NAME_PK
create unique index TABLE_NAME_PK on TABLE_NAME(ID, OUT_UTC)

GO

//// CHANGE INDEX name=TABLE_NAME_IDX0
create index TABLE_NAME_IDX0 on TABLE_NAME(TABLE_NAME_3, OUT_UTC)

GO

//// CHANGE INDEX name=TABLE_NAME_IDX1
create index TABLE_NAME_IDX1 on TABLE_NAME(TABLE_NAME_2, OUT_UTC)

GO
