create table Test (
	KEY int,
	STR_VAL varchar(20),
	STR_VAL2 char(10),
	DOUBLE_VAL number(20, 4),
	DATE_VAL datetime
);

insert into Test values (1, 'abc', 'def', 5.25, '2010-05-12 00:00:00');
insert into Test values (2, 'hello', 'def', 3.0, '2010-05-13 00:00:00');
insert into Test values (3, 'this is a test', 'def', -.2, '2010-05-14 00:00:00');
insert into Test values (4, 'more rows', 'def', -17.5, '2010-05-15 00:00:00');
