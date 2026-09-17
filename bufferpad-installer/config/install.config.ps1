@{
    InstallRoot = 'C:\bufferpad'
    AdminUsername = 'admin'
    InitialAdminPassword = $env:BUFFERPAD_ADMIN_PASSWORD

    FrontendPort = 18088
    BackendPort  = 9001
    MysqlPort    = 3306

    DatabaseName      = 'wms_opc'
    MysqlRootPassword = $env:BUFFERPAD_MYSQL_ROOT_PASSWORD
    MysqlUser         = 'root'
    MysqlPassword     = $env:BUFFERPAD_DB_PASSWORD

    MysqlServiceName   = 'BufferPadMySQL'
    BackendServiceName = 'BufferPadBackend'
    NginxServiceName   = 'BufferPadNginx'

    BackendMonitorTaskName        = 'BufferPadBackendMonitor'
    BackendMonitorIntervalSeconds = 15

    JavaPackagePattern  = '*jdk*17*.zip'
    MysqlPackagePattern = 'mysql-8.0.*-winx64.zip'
    NginxPackagePattern = 'nginx*.zip'
    WinSWPackagePattern = 'WinSW-x64.exe'
    VCRedistPattern     = 'VC_redist.x64.exe'

    BackendJarName = 'opc.jar'
    DbDumpFileName = 'wms_opc.sql'

    JavaXms = '256m'
    JavaXmx = '512m'
}
