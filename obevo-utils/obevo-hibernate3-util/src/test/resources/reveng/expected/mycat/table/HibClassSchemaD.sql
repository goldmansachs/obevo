//// CHANGE name=change0
create table HibClassSchemaD (
        id integer not null,
        name varchar(255),
        hibClassC_id integer,
        primary key (id)
    ) lock datarows
GO

//// CHANGE FK name=FKB606BC2CFE2895A5
alter table HibClassSchemaD 
        add constraint FKB606BC2CFE2895A5 
        foreign key (hibClassC_id) 
        references HibClassSchemaC
GO
