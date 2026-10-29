# Original procedural PCM cues. Re-run from the repository root to reproduce the WAVs.
$sampleRate = 22050
$destination = Join-Path $PSScriptRoot '../src/main/resources/audio/effects'
foreach ($cue in @(@('step', 0.48), @('warning', 1.2), @('retreat', 0.85), @('jumpscare', 1.65))) {
    $kind = [string]$cue[0]
    $duration = [double]$cue[1]
    $count = [int]($sampleRate * $duration)
    $rng = [System.Random]::new(1729)
    $buffer = [System.IO.MemoryStream]::new()
    $writer = [System.IO.BinaryWriter]::new($buffer)
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes('RIFF'))
    $writer.Write([int](36 + $count * 2))
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes('WAVEfmt '))
    $writer.Write([int]16)
    $writer.Write([int16]1)
    $writer.Write([int16]1)
    $writer.Write([int]$sampleRate)
    $writer.Write([int]($sampleRate * 2))
    $writer.Write([int16]2)
    $writer.Write([int16]16)
    $writer.Write([System.Text.Encoding]::ASCII.GetBytes('data'))
    $writer.Write([int]($count * 2))
    $lowNoise = 0.0
    for ($i = 0; $i -lt $count; $i++) {
        $t = $i / [double]$sampleRate
        $noise = $rng.NextDouble() * 2 - 1
        $lowNoise = $lowNoise * 0.9 + $noise * 0.1
        $edge = [Math]::Min(1, [Math]::Min($t / 0.008, ($duration - $t) / 0.06))
        switch ($kind) {
            'step' {
                $pulse = [Math]::Exp(-$t * 26)
                if ($t -gt 0.24) { $pulse += 0.6 * [Math]::Exp(-($t - 0.24) * 26) }
                $value = $pulse * (0.7 * [Math]::Sin(2 * [Math]::PI * 58 * $t) + $lowNoise * 3)
            }
            'warning' {
                $envelope = [Math]::Pow([Math]::Sin([Math]::PI * $t / $duration), 0.6)
                $value = $envelope * (0.26 * [Math]::Sin(2 * [Math]::PI * 71 * $t) + 0.2 * [Math]::Sin(2 * [Math]::PI * 76 * $t) + $lowNoise * 1.2)
            }
            'retreat' {
                $value = (1 - $t / $duration) * ($lowNoise * 2 + 0.2 * [Math]::Sin(2 * [Math]::PI * (350 * $t - 130 * $t * $t)))
            }
            'jumpscare' {
                $envelope = [Math]::Exp(-$t * 1.4)
                $phase = 2 * [Math]::PI * (370 * $t + 160 * $t * $t)
                $value = $envelope * (0.4 * [Math]::Sin($phase) + 0.23 * [Math]::Sin($phase * 1.47) + 0.35 * $noise + 0.2 * [Math]::Sin(2 * [Math]::PI * 49 * $t))
            }
        }
        $sample = [int16]([Math]::Tanh($value * 1.5) * $edge * 26000)
        $writer.Write($sample)
    }
    $writer.Flush()
    [System.IO.File]::WriteAllBytes((Join-Path $destination "gudo-$kind.wav"), $buffer.ToArray())
    $writer.Dispose()
    $buffer.Dispose()
}
