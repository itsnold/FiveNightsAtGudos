param(
    [string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$masterDirectory = Join-Path $ProjectRoot 'asset-sources/images/continuity-2026-11-14'
$imageDirectory = Join-Path $ProjectRoot 'src/main/resources/images'
$names = @(
    'cam-01-entrance', 'cam-02-counter', 'cam-03-kitchen',
    'cam-04-stockroom', 'cam-05-corridor', 'cam-06-alley',
    'office-left', 'office-front', 'office-right'
)

# Export full frames at the base scene size; no crop or retouching is applied.
foreach ($name in $names) {
    if ($name -eq 'cam-01-entrance') {
        foreach ($suffix in @('', '-occupied')) {
            $sourceImage = [System.Drawing.Image]::FromFile((Join-Path $ProjectRoot "asset-sources/images/entrance-2026-11-14/$name$suffix.png"))
            $export = New-Object System.Drawing.Bitmap(1916, 821)
            $graphics = [System.Drawing.Graphics]::FromImage($export)
            try {
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.DrawImage($sourceImage, 0, 0, $export.Width, $export.Height)
                $export.Save((Join-Path $imageDirectory "cameras/$name$suffix.png"), [System.Drawing.Imaging.ImageFormat]::Png)
            } finally {
                $graphics.Dispose()
                $export.Dispose()
                $sourceImage.Dispose()
            }
        }
        continue
    }
    $category = if ($name.StartsWith('office-')) { 'office' } else { 'cameras' }
    $baseName = if ($category -eq 'office') { 'office-panorama' } else { $name }
    $baseImage = [System.Drawing.Image]::FromFile((Join-Path $imageDirectory "$category/$baseName.png"))
    $sourceImage = [System.Drawing.Image]::FromFile((Join-Path $masterDirectory "$name-occupied.png"))
    $export = New-Object System.Drawing.Bitmap($baseImage.Width, $baseImage.Height)
    $graphics = [System.Drawing.Graphics]::FromImage($export)
    try {
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.DrawImage($sourceImage, 0, 0, $export.Width, $export.Height)
        $export.Save((Join-Path $imageDirectory "$category/$name-occupied.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $export.Dispose()
        $sourceImage.Dispose()
        $baseImage.Dispose()
    }
}

# Keep the empty and occupied room pairs together for visual review.
$cellWidth = 840
$cellHeight = 390
$sheet = New-Object System.Drawing.Bitmap(($cellWidth * 2), ($cellHeight * $names.Count))
$graphics = [System.Drawing.Graphics]::FromImage($sheet)
$font = New-Object System.Drawing.Font('Arial', 17)
try {
    $graphics.Clear([System.Drawing.Color]::FromArgb(18, 21, 26))
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    for ($row = 0; $row -lt $names.Count; $row++) {
        $name = $names[$row]
        $category = if ($name.StartsWith('office-')) { 'office' } else { 'cameras' }
        $baseName = if ($category -eq 'office') { 'office-panorama' } else { $name }
        $pair = @("$baseName.png", "$name-occupied.png")
        for ($column = 0; $column -lt 2; $column++) {
            $roomImage = [System.Drawing.Image]::FromFile((Join-Path $imageDirectory "$category/$($pair[$column])"))
            try {
                $x = $column * $cellWidth + 8
                $y = $row * $cellHeight + 8
                $graphics.DrawImage($roomImage, $x, $y, 824, 353)
                $label = if ($column -eq 0) { "$name / empty" } else { "$name / Gudo" }
                $graphics.DrawString($label, $font, [System.Drawing.Brushes]::White, [single]$x, [single]($y + 357))
            } finally { $roomImage.Dispose() }
        }
    }
    $sheet.Save((Join-Path $ProjectRoot 'docs/assets/continuity-contact-sheet.jpg'), [System.Drawing.Imaging.ImageFormat]::Jpeg)
} finally {
    $graphics.Dispose()
    $font.Dispose()
    $sheet.Dispose()
}

Write-Output 'Exported nine occupied renders and the empty/occupied comparison sheet.'
