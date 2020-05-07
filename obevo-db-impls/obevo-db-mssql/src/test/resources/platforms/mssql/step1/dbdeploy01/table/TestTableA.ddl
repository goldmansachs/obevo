//// CHANGE name=init
CREATE TABLE ${dbdeploy01_subschemaSuffixed}TestTableA  (
    id int NOT NULL,
	tId 	int NULL,
	test	varchar(25) NULL,
	fk int NOT NULL,
	CONSTRAINT TestTableA_PK PRIMARY KEY (id)
	)
GO
-- define this fk pointing to a table earlier in the alphabet to ensure that hsql drop fks will work (due to case-insensitivity). Yes, we need a separate test around the drop table statements...
ALTER TABLE ${dbdeploy01_subschemaSuffixed}TestTableA ADD FOREIGN KEY (fk) REFERENCES ${dbdeploy01_subschemaSuffixed}TestTable(idField)
GO
CREATE INDEX IND1 ON ${dbdeploy01_subschemaSuffixed}TestTableA(test)
GO
DROP INDEX ${dbdeploy01_subschemaSuffixed}TestTableA.IND1
GO
CREATE INDEX IND1 ON ${dbdeploy01_subschemaSuffixed}TestTableA(test)
GO
