# Quick test: Send MC message to JohnA
# Run this from Windows PowerShell
# Usage: powershell -ExecutionPolicy Bypass -File .\send_mc.ps1

$tcp = New-Object Net.Sockets.TcpClient("172.26.165.8", 5190)
$stream = $tcp.GetStream()

# Frame: 5A CRC(seq+seq+type+PA) data(40) 0D
# PA command with "send JohnA MC"

# Data: "send" (10) + "JohnA MC" (30)
$data = [byte[]]@(0x73,0x65,0x6E,0x64,0x00,0x00,0x00,0x00,0x00,0x00,0x4A,0x6F,0x68,0x6E,0x41,0x20,0x4D,0x43,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00)

# Build frame: 5A + 4 CRC bytes + seq1 + seq2 + 20 + PA + 40 byte data
$full = [byte[]]@(0x5A,0x00,0x00,0x00,0x00,0x7F,0x7F,0x20,0x50,0x41) + $data

# CRC16 over bytes 5-end
$crc = 0
for ($i=5; $i -lt $full.Length; $i++) {
    $crc = $crc -bxor ($full[$i] -shl 8)
    for ($j=0; $j -lt 8; $j++) {
        if ($crc -band 0x8000) { $crc = (($crc -shl 1) -band 0xFFFF) -bxor 0x1021 }
        else { $crc = ($crc -shl 1) -band 0xFFFF }
    }
}

# Insert CRC
$full[1] = [byte](($crc -band 0xF000) -shr 8 -bor 0x01)
$full[2] = [byte](($crc -band 0x0F00) -shr 8 -bor 0x40)
$full[3] = [byte](($crc -band 0x00F0) -bor 0x01)
$full[4] = [byte](($crc -band 0x000F) -bor 0x40)

# Send
$stream.Write($full, 0, $full.Length)
$stream.WriteByte(0x0D)
Write-Host "Sent MC message"
$stream.Close()
$tcp.Close()
