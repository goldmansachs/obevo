CREATE PROCEDURE TestProc as select * from TestView
GO

create procedure TestProc;2 (@param1  int) as select * from TestView

GO
sp_procxmode 'TestProc', 'Unchained'
GO