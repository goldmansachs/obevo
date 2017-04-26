create procedure TestProc as/* Adaptive Server has expanded all '*' elements in the following statement */  select TestView.MyId, TestView.MyString, TestView.MyMyBooleanCol from TestView
GO
create procedure TestProc;2 (@param1  int) as/* Adaptive Server has expanded all '*' elements in the following statement */  select TestView.MyId, TestView.MyString, TestView.MyMyBooleanCol from TestView
GO
sp_procxmode 'TestProc', unchained
GO