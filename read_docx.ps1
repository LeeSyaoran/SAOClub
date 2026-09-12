$word = New-Object -ComObject Word.Application
$word.Visible = $false
$docPath = "d:\project code\SAOClub\Note những thứ cần cải thiện.docx"
$doc = $word.Documents.Open($docPath)
$text = $doc.Content.Text
$doc.Close()
$word.Quit()
Write-Output $text
