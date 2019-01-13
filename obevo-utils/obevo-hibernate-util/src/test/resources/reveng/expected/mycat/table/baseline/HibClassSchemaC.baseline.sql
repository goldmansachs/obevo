create table HibClassSchemaC (
        id int not null,
        col1 varchar(255) null,
        col2 varchar(255) null,
        name varchar(255) null,
        primary key (id)
    ) lock datarows
GO
create index Idx1 on HibClassSchemaC (col1, col2) lock datarows
GO