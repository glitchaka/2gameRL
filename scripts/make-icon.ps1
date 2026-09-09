param(
    [Parameter(Mandatory=$true)][string]$Output
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$size = 256
$pixelFormat = [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
$bmp = [System.Drawing.Bitmap]::new($size,$size,$pixelFormat)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

$bounds = [System.Drawing.Rectangle]::new(0,0,$size,$size)
$bg = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
    $bounds,
    [System.Drawing.Color]::FromArgb(255,7,24,39),
    [System.Drawing.Color]::FromArgb(255,22,11,43),
    45.0
)
$g.FillRectangle($bg,0,0,$size,$size)

$gridPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(38,76,223,255),1.0)
for($i=24;$i -lt 246;$i+=22){
    $g.DrawLine($gridPen,$i,16,$i,240)
    $g.DrawLine($gridPen,16,$i,240,$i)
}

$cyanPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255,67,244,255),6.0)
$bluePen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255,49,148,255),3.0)
$magentaPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255,192,59,255),3.0)
$g.DrawRectangle($cyanPen,8,8,239,239)
$g.DrawLine($bluePen,128,38,217,89)
$g.DrawLine($magentaPen,217,89,128,218)
$g.DrawLine($bluePen,128,218,39,89)
$g.DrawLine($cyanPen,39,89,128,38)

$cyanBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255,56,234,247))
$blueBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255,51,141,255))
$magentaBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255,211,55,255))
$shadowBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(180,0,0,0))
$textBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255,226,247,255))
$g.FillRectangle($cyanBrush,25,75,10,10)
$g.FillRectangle($blueBrush,220,180,9,9)
$g.FillRectangle($magentaBrush,217,61,7,7)

$font = [System.Drawing.Font]::new('Segoe UI',66.0,[System.Drawing.FontStyle]::Bold,[System.Drawing.GraphicsUnit]::Pixel)
$format = [System.Drawing.StringFormat]::new()
$format.Alignment = [System.Drawing.StringAlignment]::Center
$format.LineAlignment = [System.Drawing.StringAlignment]::Center
$rect = [System.Drawing.RectangleF]::new(0,61,256,140)
$shadowRect = [System.Drawing.RectangleF]::new(3,65,256,140)
$g.DrawString('2RL',$font,$shadowBrush,$shadowRect,$format)
$g.DrawString('2RL',$font,$textBrush,$rect,$format)

$dir = Split-Path -Parent $Output
if($dir -and -not (Test-Path $dir)){New-Item -ItemType Directory -Force -Path $dir | Out-Null}

$hIcon = $bmp.GetHicon()
$icon = [System.Drawing.Icon]::FromHandle($hIcon)
$stream = [System.IO.File]::Open($Output,[System.IO.FileMode]::Create,[System.IO.FileAccess]::Write,[System.IO.FileShare]::None)
try {
    $icon.Save($stream)
} finally {
    $stream.Dispose()
    $icon.Dispose()
    $g.Dispose()
    $bmp.Dispose()
    $bg.Dispose()
    $font.Dispose()
    $format.Dispose()
    $gridPen.Dispose()
    $cyanPen.Dispose()
    $bluePen.Dispose()
    $magentaPen.Dispose()
    $cyanBrush.Dispose()
    $blueBrush.Dispose()
    $magentaBrush.Dispose()
    $shadowBrush.Dispose()
    $textBrush.Dispose()
}

if(-not (Test-Path $Output) -or (Get-Item $Output).Length -lt 100){
    throw "No se generó un archivo ICO válido en $Output"
}
Write-Host "[2gameRL] Icono 2RL generado: $Output"
