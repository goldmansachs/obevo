//// CHANGE name=change0
create table HibClassSchemaC (
        id integer not null,
        col1 varchar(255),
        col2 varchar(255),
        name varchar(255),
        primary key (id)
    ) lock datarows
GO

//// CHANGE INDEX name=Idx1
create index Idx1 on HibClassSchemaC (col1, col2)
GO
