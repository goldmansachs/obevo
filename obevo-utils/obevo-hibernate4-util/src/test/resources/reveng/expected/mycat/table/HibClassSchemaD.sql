//// CHANGE name=change0
create table HibClassSchemaD (
        id integer not null,
        name varchar(255),
        hibClassC_id integer,
        primary key (id)
    ) lock datarows
GO

//// CHANGE FK name=FK_exubptj7fyn2ut1wkklqgadfe
alter table HibClassSchemaD 
        add constraint FK_exubptj7fyn2ut1wkklqgadfe 
        foreign key (hibClassC_id) 
        references HibClassSchemaC
GO
