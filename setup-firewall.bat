@echo off
echo ====================================================================
echo  The Infected Hour - Setup Windows Firewall for Co-op Multiplayer
echo ====================================================================
echo.

:: Check for Administrator permissions
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo [!] Requesting Administrator privileges...
    powershell -Command "Start-Process cmd -ArgumentList '/c \"%~f0\"' -Verb runAs"
    exit /b
)

echo [*] Adding Windows Firewall Rule: KryoNet TCP (Port 54555)...
powershell -Command "New-NetFirewallRule -DisplayName 'The Infected Hour (TCP 54555)' -Direction Inbound -LocalPort 54555 -Protocol TCP -Action Allow -Profile Any" >nul

echo [*] Adding Windows Firewall Rule: KryoNet UDP & Discovery (Ports 54777, 54778)...
powershell -Command "New-NetFirewallRule -DisplayName 'The Infected Hour (UDP 54777-54778)' -Direction Inbound -LocalPort 54777,54778 -Protocol UDP -Action Allow -Profile Any" >nul

echo [*] Adding Windows Firewall Rule: Spring Boot Backend API (Port 8080)...
powershell -Command "New-NetFirewallRule -DisplayName 'The Infected Hour (Backend TCP 8080)' -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow -Profile Any" >nul

echo.
echo ====================================================================
echo  [SUCCESS] All necessary ports have been opened for LAN and Radmin VPN!
echo ====================================================================
echo.
pause
