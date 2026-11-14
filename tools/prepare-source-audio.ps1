param([string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent))

$ErrorActionPreference = 'Stop'
$sourceDirectory = Join-Path $ProjectRoot 'asset-sources/audio'
$outputDirectory = Join-Path $ProjectRoot 'src/main/resources/audio'
$workDirectory = Join-Path $ProjectRoot 'target/audio-work'
New-Item -ItemType Directory -Force -Path $workDirectory | Out-Null
# Preserve the archive. Export bounded PCM clips so long source tracks cannot spill across scenes.
# Source, runtime path, start (seconds), duration, fade-in, fade-out.
$cues = @(
    @('ambience/ambience2.wav', 'ambience/room-tone.wav', 0, 30, 0.12, 0.30),
    @('effects/static.wav', 'ambience/camera-hiss.wav', 0, 10, 0.04, 0.08),
    @('ambience/ColdPresc B.wav', 'ambience/late-shift.wav', 12, 30, 0.60, 0.80),
    @('character/Vocals_Breaths_S_35972008.wav', 'ambience/gudo-breath.wav', 0, 5.9, 0.10, 0.25),
    @('effects/COMPUTER_DIGITAL_L2076505.wav', 'effects/flashlight-on.wav', 0, 0.16, 0.005, 0.04),
    @('effects/COMPUTER_DIGITAL_L2076505.wav', 'effects/flashlight-off.wav', 0.4, 0.12, 0.005, 0.04),
    @('character/garble1.wav', 'effects/garble1.wav', 0, 2, 0.008, 0.06),
    @('character/garble2.wav', 'effects/garble2.wav', 0, 2, 0.008, 0.06),
    @('character/garble3.wav', 'effects/garble3.wav', 0, 2, 0.008, 0.06),
    @('effects/COMPUTER_DIGITAL_L2076505.wav', 'effects/camera-garble-digital.wav', 0, 2, 0.008, 0.06),
    @('effects/OVEN-DRA_1_GEN-HDF18119.wav', 'effects/metal-creak.wav', 0, 2.6, 0.03, 0.30),
    @('effects/OVEN-DRA_2_GEN-HDF18120.wav', 'effects/metal-rattle.wav', 0, 2.2, 0.03, 0.30),
    @('effects/COMPUTER_DIGITAL_L2076505.wav', 'effects/low-power.wav', 1, 0.65, 0.01, 0.15),
    @('character/deep steps.wav', 'effects/deep-steps.wav', 0, 7.82, 0.008, 0.08),
    @('effects/knock2.wav', 'effects/entrance-knock.wav', 0, 2.8, 0.008, 0.25),
    @('effects/DOOR_POUNDING_ME_D0291401.wav', 'effects/corridor-pound.wav', 0, 2.5, 0.008, 0.25),
    @('character/whispering2.wav', 'effects/entrance-whisper.wav', 0, 2.4, 0.08, 0.30),
    @('character/running fast3.wav', 'effects/attack-rush.wav', 0, 1.8, 0.015, 0.20),
    @('character/running fast3.wav', 'effects/retreat-fast.wav', 0, 2.23, 0.008, 0.12),
    @('character/XSCREAM2.wav', 'effects/caught-scream2.wav', 0, 2.05, 0.005, 0.15)
)

foreach ($cue in $cues) {
    $inputPath = Join-Path $sourceDirectory $cue[0]
    $outputPath = Join-Path $outputDirectory $cue[1]
    $trimmedPath = Join-Path $workDirectory ([IO.Path]::GetFileName($outputPath))
    $fadeStart = ([double]$cue[3] - [double]$cue[5]).ToString('0.###', [Globalization.CultureInfo]::InvariantCulture)
    $filters = "afade=t=in:d=$($cue[4]),afade=t=out:st=${fadeStart}:d=$($cue[5]),alimiter=limit=0.85:level=false"
    & ffmpeg -hide_banner -loglevel error -y -ss $cue[2] -i $inputPath -t $cue[3] -af $filters -ar 22050 -ac 1 -c:a pcm_s16le $trimmedPath
    if ($LASTEXITCODE -ne 0) { throw "Could not export $inputPath" }
    # Normalize peak levels before the game's individual gains: quiet source clicks and whispers
    # otherwise disappear under the fan. Runtime gains still keep ambience below encounter cues.
    # Windows PowerShell represents native stderr (including ffmpeg's normal statistics) as errors.
    $ErrorActionPreference = 'Continue'
    try { $peakReport = (& ffmpeg -hide_banner -i $trimmedPath -af volumedetect -f null NUL 2>&1) | Out-String }
    finally { $ErrorActionPreference = 'Stop' }
    if ($peakReport -notmatch 'max_volume: (-?[0-9.]+) dB') { throw "No audio peak in $inputPath" }
    $peak = [double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture)
    $gain = (-3 - $peak).ToString('0.###', [Globalization.CultureInfo]::InvariantCulture)
    & ffmpeg -hide_banner -loglevel error -y -i $trimmedPath -af "volume=${gain}dB" -ar 22050 -ac 1 -c:a pcm_s16le $outputPath
    if ($LASTEXITCODE -ne 0) { throw "Could not normalize $outputPath" }
}
Write-Output "Prepared $($cues.Count) source-based WAVs; archive files unchanged."
