#!/bin/bash
docker exec saoclub-sqlserver-1 /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'SaoClub@2024' -C -Q "SELECT name FROM sys.databases;"
echo "---"
docker exec saoclub-sqlserver-1 /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'SaoClub@2024' -C -d SAOClub -Q "ALTER TABLE chi_tiet_san_pham ADD da_xoa BIT NOT NULL DEFAULT 0;"
echo "---"
docker exec saoclub-sqlserver-1 /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'SaoClub@2024' -C -d SAOClub -Q "SELECT name FROM sys.columns WHERE object_id = OBJECT_ID('chi_tiet_san_pham') AND name = 'da_xoa';"
echo "---"
docker restart saoclub-backend-1
