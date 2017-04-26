exec  sp_addtype 'Boolean' , 'tinyint' , nonull
GO
sp_bindrule 'booleanRule', 'Boolean'
GO