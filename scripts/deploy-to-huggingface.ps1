param (
    [Parameter(Mandatory=$true)]
    [string]$Username,
    [Parameter(Mandatory=$false)]
    [string]$SpaceName = "mediadock-backend",
    [Parameter(Mandatory=$false)]
    [string]$Token
)

Write-Host "Deploying MediaDock Backend to Hugging Face Space: $Username/$SpaceName" -ForegroundColor Cyan

$remoteUrl = if ($Token) {
    "https://${Username}:${Token}@huggingface.co/spaces/${Username}/${SpaceName}"
} else {
    "https://huggingface.co/spaces/${Username}/${SpaceName}"
}

# Remove old hf remote if exists
git remote remove hf 2>$null
git remote add hf $remoteUrl

Write-Host "Pushing backend to Hugging Face..." -ForegroundColor Green
git subtree push --prefix backend/snagbox hf main

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Successfully pushed to Hugging Face Spaces!" -ForegroundColor Green
Write-Host "Space Dashboard: https://huggingface.co/spaces/$Username/$SpaceName" -ForegroundColor Cyan
Write-Host "Your Public API URL: https://$Username-$SpaceName.hf.space" -ForegroundColor Yellow
Write-Host "Paste this Public API URL into your MediaDock app Settings!" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Green
