//// CHANGE name=init
CREATE TABLE TestTableA  (
    id int NOT NULL,
	tId 	int NULL,
	test	varchar(25) NULL,
	fk int NOT NULL,
	CONSTRAINT PK PRIMARY KEY (id)
	) LOCK DATAROWS
GO
-- define this fk pointing to a table earlier in the alphabet to ensure that hsql drop fks will work (due to case-insensitivity). Yes, we need a separate test around the drop table statements...
ALTER TABLE TestTableA ADD FOREIGN KEY (fk) REFERENCES TestTable(idField)
GO
CREATE INDEX IND1 ON TestTableA(test)
GO
DROP INDEX TestTableA.IND1
GO
CREATE INDEX IND1 ON TestTableA(test)
GO
