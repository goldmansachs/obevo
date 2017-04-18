create proc SpWithTemp (@MaxCount  int) as
--$Header: /home/cvs/gdtech/costar/src/db/procs/DelExcessSelectTrades.eqd,v 1.2 2010/08/13 18:15:51 jayava Exp $
begin
    select * from #MyTemp
end
GO
sp_procxmode 'SpWithTemp', 'Unchained'
GO