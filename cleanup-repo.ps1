# Cleanup script for seckill-system repository
# Removes build artifacts, IDE config, redundant files, updates .gitignore, commits, and runs mvn clean

Write-Host "Starting repository cleanup..."

# Files/folders to remove (git-tracked or not)
$pathsToRemove = @('target', '.idea', 'HELP.md', 'application-no-es.yml', 'seckill-system-0.0.1-SNAPSHOT.jar', 'seckill-system-0.0.1-SNAPSHOT.jar.original')

# Ensure we're at repo root
Set-Location -Path $PSScriptRoot

# Git remove (ignore-unmatch to avoid failing if not tracked)
foreach ($p in $pathsToRemove) {
    Write-Host "git rm --ignore-unmatch $p"
    git rm --ignore-unmatch $p 2>$null
}

# Remove leftover files/directories from filesystem
foreach ($p in $pathsToRemove) {
    if (Test-Path $p) {
        Write-Host "Removing filesystem path: $p"
        Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $p
    }
}

# Remove any jar files matching pattern at repo root
Get-ChildItem -Path $PSScriptRoot -Filter "seckill-system-0.0.1-SNAPSHOT.jar*" -File -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "Removing file: $($_.FullName)"
    Remove-Item -Force -ErrorAction SilentlyContinue $_.FullName
}

# Ensure .gitignore contains *.jar
$gitignore = Join-Path $PSScriptRoot '.gitignore'
if (-not (Test-Path $gitignore)) { "" | Out-File -FilePath $gitignore -Encoding utf8 }
$giContent = Get-Content $gitignore -ErrorAction SilentlyContinue
if ($giContent -notcontains "*.jar") {
    Add-Content -Path $gitignore -Value "*.jar"
    Write-Host "Appended '*.jar' to .gitignore"
} else {
    Write-Host "'.gitignore' already contains '*.jar'"
}

# Stage .gitignore in case it changed
git add .gitignore 2>$null

# Commit changes if any
$changes = git status --porcelain
if ($changes) {
    git commit -m "Remove redundant files and duplicates" -m "Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>" 2>$null
    Write-Host "Committed repository cleanup changes"
} else {
    Write-Host "No changes to commit"
}

# Run mvn clean to verify
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    Write-Host "Running 'mvn -q clean'..."
    mvn -q clean
    Write-Host "mvn clean finished"
} else {
    Write-Host "Maven not found in PATH. Please run 'mvn -q clean' manually if you want to verify the build artifacts are removed."
}

Write-Host "Cleanup script completed. Please review 'git status' locally and push if satisfied."