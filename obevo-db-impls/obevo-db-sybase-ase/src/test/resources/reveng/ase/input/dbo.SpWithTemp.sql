create proc SpWithTemp (@MaxCount  int) as
begin
    select * from #MyTemp
end
GO
sp_procxmode 'SpWithTemp', 'Unchained'
GO

