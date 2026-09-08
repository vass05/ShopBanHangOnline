param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MvnArgs
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$PSScriptRootResolved = Split-Path -Parent $MyInvocation.MyCommand.Definition
$MavenHome = Join-Path $PSScriptRootResolved ".maven\apache-maven-3.9.9"
$M2Conf = Join-Path $MavenHome "bin\m2.conf"
$BootJar = (Get-ChildItem -Path (Join-Path $MavenHome "boot") -Filter "plexus-classworlds-*.jar" | Select-Object -First 1).FullName

$BootJarRel = ".maven/apache-maven-3.9.9/boot/plexus-classworlds-2.8.0.jar"
$M2ConfRel = ".maven/apache-maven-3.9.9/bin/m2.conf"
$MavenHomeRel = ".maven/apache-maven-3.9.9"

$JavaArgs = @(
    "-Dclassworlds.conf=$M2ConfRel",
    "-Dmaven.home=$MavenHomeRel",
    "-Dmaven.multiModuleProjectDirectory=.",
    "-classpath",
    $BootJarRel,
    "org.codehaus.plexus.classworlds.launcher.Launcher"
) + $MvnArgs

& java $JavaArgs
exit $LASTEXITCODE
