//// CHANGE name=init
CREATE TABLE DefaultColTest  (
	id 	int IDENTITY NOT NULL,
	default_str	varchar(25) DEFAULT '123' NULL,
	default_nn_str	varchar(25) DEFAULT '123' NOT NULL,
	default_curdate	datetime DEFAULT getdate() NULL,
	default_date1	datetime DEFAULT '20 Jan 2005 12:12:12' NULL,
	default_date2	datetime DEFAULT 'Jan 20 2005' NULL
	)
GO
ALTER TABLE DefaultColTest ADD CONSTRAINT PK_DefaultColTest PRIMARY KEY (id)
GO
