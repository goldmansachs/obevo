create table TABLE1
(
    ID bigint not null,
    APPLICATION_ID bigint not null,
    ROLE_ID bigint not null,
    USER_ID bigint not null,
    CREATED_BY varchar(64),
    CREATED_DATE_UTC timestamp,
    LAST_UPDATED_BY varchar(64),
    IN_UTC timestamp not null,
    OUT_UTC timestamp not null
);

