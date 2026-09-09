param(
    [Parameter(Mandatory=$true)][string]$Output
)

Add-Type -AssemblyName System.Drawing
$size = 256
$bmp = New-Object System.Drawing.Bitmap $size,$size,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

$bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Rectangle 0,0,$size,$size),
    ([System.Drawing.Color]::FromArgb(255,7,24,39)),
    ([System.Drawing.Color]::FromArgb(255,22,11,43)),
    45.0
)
$g.FillRectangle($bg,0,0,$size,$size)

$gridPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(38,76,223,255)),1
for($i=24;$i -lt 246;$i+=22){$g.DrawLine($gridPen,$i,16,$i,240);$g.DrawLine($gridPen,16,$i,240,$i)}

$cyanPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255,67,244,255)),6
$bluePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255,49,148,255)),3
$magentaPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255,192,59,255)),3
$g.DrawRectangle($cyanPen,8,8,239,239)
$g.DrawLine($bluePen,128,38,217,89);$g.DrawLine($magentaPen,217,89,128,218);$g.DrawLine($bluePen,128,218,39,89);$g.DrawLine($cyanPen,39,89,128,38)

$g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,56,234,247))),25,75,10,10)
$g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,51,141,255))),220,180,9,9)
$g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,211,55,255))),217,61,7,7)

$font = New-Object System.Drawing.Font 'Segoe UI',66,[System.Drawing.FontStyle]::Bold,[System.Drawing.GraphicsUnit]::Pixel
$format = New-Object System.Drawing.StringFormat
$format.Alignment = [System.Drawing.StringAlignment]::Center
$format.LineAlignment = [System.Drawing.StringAlignment]::Center
$rect = New-Object System.Drawing.RectangleF 0,61,256,140
$shadowRect = New-Object System.Drawing.RectangleF 3,65,256,140
$g.DrawString('2RL',$font,(New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(180,0,0,0))),$shadowRect,$format)
$g.DrawString('2RL',$font,(New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255,226,247,255))),$rect,$format)

$dir = Split-Path -Parent $Output
if($dir -and -not (Test-Path $dir)){New-Item -ItemType Directory -Force -Path $dir | Out-Null}
$hIcon = $bmp.GetHicon()
$icon = [System.Drawing.Icon]::FromHandle($hIcon)
$stream = [System.IO.File]::Open($Output,[System.IO.FileMode]::Create)
try {$icon.Save($stream)} finally {$stream.Dispose();$icon.Dispose();$g.Dispose();$bmp.Dispose();$bg.Dispose();$font.Dispose();$format.Dispose();$gridPen.Dispose();$cyanPen.Dispose();$bluePen.Dispose();$magentaPen.Dispose()}
Write-Host "[2gameRL] Icono 2RL generado: $Output"
