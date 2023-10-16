@echo off
REM Function to print usage instructions
:usage
echo Usage: %~nx0 <command-to-run-in-container>
echo Example: %~nx0 ping 192.168.1.1
exit /b 1

REM Check if there is at least one argument
if "%~#" lss "1" (
  call :usage
)

REM Find the containers based on the image name
for /f %%i in ('docker container ls --filter "ancestor=opennms/lokahi-minion" -q') do set "container_id=%%i"
setlocal enabledelayedexpansion
set "container_count=0"
for /f %%i in ('docker container ls --filter "ancestor=opennms/lokahi-minion"') do (
  set /a "container_count+=1"
  set "container_list=!container_list!%%i"
)
endlocal

REM Display a warning if there are multiple containers with the same ancestor
if !container_count! geq 2 (
  echo Warning: Multiple containers found with ancestor 'opennms/lokahi-minion':
  echo !container_list!
  exit /b 1
)

REM Find the first container ID based on the image name
if not defined container_id (
  echo No container found matching the specified image name.
  exit /b 1
)

REM Run the command in the specified container
docker exec -it !container_id! /bin/bash ./bin/client -- %*
