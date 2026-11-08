param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$FirebaseCommand,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$FirebaseArguments
)

$javaCandidates = @(
    $env:JAVA_HOME,
    'D:\Android\Android Studio\jbr',
    'C:\Program Files\Android\Android Studio\jbr'
)

$javaHome = $javaCandidates |
    Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe')) } |
    Select-Object -First 1

if (-not $javaHome) {
    throw 'Java was not found. Install Android Studio or set JAVA_HOME to its bundled jbr folder.'
}

$env:JAVA_HOME = $javaHome
$env:PATH = (Join-Path $javaHome 'bin') + ';' + $env:PATH

& firebase.cmd $FirebaseCommand @FirebaseArguments
exit $LASTEXITCODE
