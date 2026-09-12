Add-Type -Path "C:/Users/huydo/.m2/repository/com/microsoft/sqlserver/mssql-jdbc/13.2.0.jre11/mssql-jdbc-13.2.0.jre11.jar"
$url = "jdbc:sqlserver://localhost:1433;databaseName=QLBanMayTinh;encrypt=true;trustServerCertificate=true"
$conn = New-Object Microsoft.sqlserver.jdbc.SQLServerConnection($url, "sa", "SaoClub@2024")
try {
    $conn.Connect()
    Write-Host "JDBC SUCCESS"
    $conn.Close()
} catch {
    Write-Host "JDBC FAILED: $($_.Exception.Message)"
}
