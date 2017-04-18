exec  sp_addtype 'Boolean2' , 'tinyint' , nonull
GO
sp_bindrule 'booleanRule2', 'Boolean2'
GO