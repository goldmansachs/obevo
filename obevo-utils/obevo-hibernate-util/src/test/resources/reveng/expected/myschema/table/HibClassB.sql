//// CHANGE name=init
create table HibClassB (
        id int not null,
        col1 varchar(255) null,
        col2 varchar(255) null,
        name varchar(255) null,
        primary key (id)
    ) lock datarows
GO

//// CHANGE INDEX name=index9
create index Idx1 on HibClassB (col1, col2) lock datarows
GO
