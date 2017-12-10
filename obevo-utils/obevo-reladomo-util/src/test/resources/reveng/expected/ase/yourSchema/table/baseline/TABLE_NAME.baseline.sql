create table TABLE_NAME
(
ID bigint not null,
TABLE_NAME_3 bigint not null,
TABLE_NAME_2 bigint not null,
OUT_UTC datetime not null
)

GO
create unique index TABLE_NAME_PK on TABLE_NAME(ID, OUT_UTC)

GO
create index TABLE_NAME_IDX0 on TABLE_NAME(TABLE_NAME_3, OUT_UTC)

GO
create index TABLE_NAME_IDX1 on TABLE_NAME(TABLE_NAME_2, OUT_UTC)

GO