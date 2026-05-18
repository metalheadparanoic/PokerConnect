New-Item -ItemType Directory -Force -Path 'client/src/main/resources/images' | Out-Null
$wc = New-Object System.Net.WebClient
$wc.DownloadFile('https://upload.wikimedia.org/wikipedia/commons/9/96/World_Poker_Tour_table_1.jpg','client/src/main/resources/images/table.jpg')
Write-Host 'Downloaded to client/src/main/resources/images/table.jpg'