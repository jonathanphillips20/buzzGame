@echo off
start "Server" cmd.exe /k "set classpath=%cd%\bin & java Server 5555 3 & pause"


