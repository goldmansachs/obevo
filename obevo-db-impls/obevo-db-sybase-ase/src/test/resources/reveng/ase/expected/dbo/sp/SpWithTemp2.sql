create proc SpWithTemp2 (@MaxCount  int) as
begin
    select * from #MyTemp
end
GO
sp_procxmode 'SpWithTemp2', 'Unchained'
GO