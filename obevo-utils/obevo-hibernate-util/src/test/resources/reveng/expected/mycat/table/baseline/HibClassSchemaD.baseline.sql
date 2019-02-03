create table HibClassSchemaD (
        id int not null,
        name varchar(255) null,
        hibClassC_id int null,
        primary key (id)
    ) lock datarows
GO
alter table HibClassSchemaD 
        add constraint FKB606BC2C47AA6F64
        foreign key (hibClassC_id) 
        references mycat.HibClassSchemaC lock datarows
GO