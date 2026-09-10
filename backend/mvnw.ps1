param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MvnArgs
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$PSScriptRootResolved = Split-Path -Parent $MyInvocation.MyCommand.Definition
$MavenHomeRel = "../.maven/apache-maven-3.9.9"
$M2ConfRel = "$MavenHomeRel/bin/m2.conf"
$BootJarRel = "$MavenHomeRel/boot/plexus-classworlds-2.8.0.jar"

$JavaArgs = @(
    "-Dclassworlds.conf=$M2ConfRel",
    "-Dmaven.home=$MavenHomeRel",
    "-Dmaven.multiModuleProjectDirectory=$PSScriptRootResolved",
    "-classpath",
    $BootJarRel,
    "org.codehaus.plexus.classworlds.launcher.Launcher"
) + $MvnArgs

& java $JavaArgs
exit $LASTEXITCODE
