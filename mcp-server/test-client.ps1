$client = New-Object System.Net.Sockets.TcpClient('127.0.0.1', 5136)
$stream = $client.GetStream()
$writer = New-Object System.IO.StreamWriter($stream)
$reader = New-Object System.IO.StreamReader($stream)

Write-Host "Sending 'GET TIME' command..."
$writer.WriteLine('GET TIME')
$writer.Flush()
$response = $reader.ReadLine()
Write-Host "Server response: $response"

$client.Close()
