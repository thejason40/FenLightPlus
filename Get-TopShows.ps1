[CmdletBinding()]
param(
    [string]$ApiKey = $env:STREAMING_API_KEY,

    [string[]]$Countries = @('gb', 'us', 'ca', 'th', 'au', 'de'),

    [hashtable]$ServiceMap = @{
        gb = @('netflix', 'prime', 'disney', 'hbo', 'apple')
        us = @('netflix', 'prime', 'disney', 'hbo', 'hulu', 'apple')
        ca = @('netflix', 'prime', 'disney', 'apple', 'crave')
        th = @('netflix', 'prime', 'hbo', 'apple')
        au = @('netflix', 'prime', 'disney', 'hbo', 'apple')
        de = @('netflix', 'prime', 'disney', 'hbo', 'apple')
    },

    [string[]]$ExcludeServices = @('crunchyroll'),

    [string]$OutputFolder = (Join-Path $PSScriptRoot 'TopShows'),

    [int]$MaxCallsPerRun = 50,

    [switch]$DiscoverServices,
    [switch]$SplitByType,
    [switch]$Probe,
    [string]$ProbeCountry = 'gb',
    [string]$ProbeService = 'netflix',

    [switch]$SaveRaw,
    [switch]$UseRapidApi
)

if ([string]::IsNullOrWhiteSpace($ApiKey)) {
    Write-Error "No API key supplied. Set `$env:STREAMING_API_KEY or pass -ApiKey."
    exit 1
}

$TopSupported = @('netflix', 'prime', 'disney', 'apple', 'hbo', 'crunchyroll', 'crave', 'hulu')

$Attribution = [ordered]@{
    text = 'Streaming data by Movie of the Night (Streaming Availability API)'
    url  = 'https://www.movieofthenight.com/about/api'
}

# --- API connection settings -------------------------------------------------
if ($UseRapidApi) {
    $baseUrl = 'https://streaming-availability.p.rapidapi.com'
    $headers = @{ 'X-RapidAPI-Key' = $ApiKey }
}
else {
    $baseUrl = 'https://api.movieofthenight.com/v4'
    $headers = @{ 'X-API-Key' = $ApiKey }
}

# --- Paths and folders ---------------------------------------------------------
if (-not [System.IO.Path]::IsPathRooted($OutputFolder)) {
    $OutputFolder = Join-Path (Get-Location).Path $OutputFolder
}

$topFolder  = Join-Path $OutputFolder 'top'
$histFolder = Join-Path $OutputFolder 'history'
$rawFolder  = Join-Path $OutputFolder 'raw'
foreach ($f in @($OutputFolder, $topFolder, $histFolder)) {
    if (-not (Test-Path $f)) { New-Item -ItemType Directory -Path $f -Force | Out-Null }
}
if (($SaveRaw -or $Probe) -and -not (Test-Path $rawFolder)) {
    New-Item -ItemType Directory -Path $rawFolder -Force | Out-Null
}

$now          = Get-Date
$today        = $now.ToString('yyyy-MM-dd')
$timestamp    = $now.ToString('yyyy-MM-dd-HHmmss')
$fetchedAt    = $now.ToString('yyyy-MM-ddTHH:mm:sszzz')

$mapFile             = Join-Path $OutputFolder 'service-map.json'
$serviceMapWasPassed = $PSBoundParameters.ContainsKey('ServiceMap')
$script:StopRun      = $false
$script:CallsThisRun = 0

# --- JSON helper ------------
function Write-JsonFile {
    param($Object, [string]$Path)
    $json = ConvertTo-Json -InputObject $Object -Depth 8
    [System.IO.File]::WriteAllText($Path, $json, (New-Object System.Text.UTF8Encoding($false)))
}

# --- API request --------------------------------
function Invoke-ApiRequest {
    param(
        [string]$PathAndQuery,
        [string]$Label,
        [string]$RawName
    )

    if ($script:CallsThisRun -ge $MaxCallsPerRun) {
        Write-Warning "Per-run call limit reached ($MaxCallsPerRun). Stopping."
        $script:StopRun = $true
        return
    }

    $script:CallsThisRun++

    Write-Host "Fetching $Label..." -ForegroundColor Cyan

    try {
        $webResponse = Invoke-WebRequest -Uri "$baseUrl$PathAndQuery" -Headers $headers -Method Get -UseBasicParsing -ErrorAction Stop
    }
    catch {
        $status = $null
        if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
        Write-Warning "  Failed for $Label : $($_.Exception.Message)"
        if ($status -in 401, 403, 429) {
            Write-Warning "  HTTP $status - stopping the run so no more calls are used."
            $script:StopRun = $true
        }
        return
    }

    $rawJson = [System.Text.Encoding]::UTF8.GetString($webResponse.RawContentStream.ToArray())

    if ($SaveRaw -or $Probe) {
        $rawPath = Join-Path $rawFolder "$RawName-$timestamp.json"
        Set-Content -Path $rawPath -Value $rawJson -Encoding UTF8
        Write-Host "  Raw JSON saved: $rawPath" -ForegroundColor DarkGray
    }

    return $rawJson
}

# --- Top shows for one country + service (+ optional type) -----------------------------
function Get-TopShows {
    param(
        [string]$Country,
        [string]$Service,
        [string]$Type    # '' = omit show_type (movies + series together)
    )

    $path = "/shows/top?country=$Country&service=$Service"
    if ($Type) { $path += "&show_type=$Type" }

    $label    = if ($Type) { $Type } else { 'movies+series' }
    $typePart = if ($Type) { $Type } else { 'all' }

    $json = Invoke-ApiRequest -PathAndQuery $path `
        -Label "top $label on $Service ($($Country.ToUpper()))" `
        -RawName "$Country-$Service-$typePart"
    if (-not $json) { return }

    ($json | ConvertFrom-Json) | ForEach-Object { $_ }
}

# --- Service discovery -------------------------------------------
function Update-ServiceMap {
    $json = Invoke-ApiRequest -PathAndQuery '/countries' -Label 'country + service list' -RawName 'countries'
    if (-not $json) { return }

    $all = $json | ConvertFrom-Json
    $map = [ordered]@{}
    foreach ($prop in ($all.PSObject.Properties | Sort-Object Name)) {
        $ids = @($prop.Value.services | ForEach-Object { $_.id })
        $map[$prop.Name] = @($ids | Where-Object { ($TopSupported -contains $_) -and ($ExcludeServices -notcontains $_) })
    }

    Write-JsonFile -Object $map -Path $mapFile
    return $map
}

if ($DiscoverServices) {
    $map = Update-ServiceMap
    if (-not $map) { exit 1 }

    Write-Host ""
    Write-Host "Services with top-10 lists, per country (excluding: $($ExcludeServices -join ', ')):" -ForegroundColor Yellow
    foreach ($code in $map.Keys) {
        '{0,-3} ({1}) {2}' -f $code, $map[$code].Count, ($map[$code] -join ', ')
    }

    Write-Host ""
    $perDay = 0
    foreach ($c in $Countries) {
        $k = $c.ToLower()
        if ($map.Contains($k)) { $perDay += $map[$k].Count } else { Write-Warning "'$c' is not a supported country in the API." }
    }
    if ($SplitByType) { $perDay = $perDay * 2 }
    Write-Host ("Your -Countries ({0}) would use {1} calls/day = about {2} per 30 days." -f ($Countries -join ','), $perDay, ($perDay * 30)) -ForegroundColor Green
    Write-Host "Saved: $mapFile" -ForegroundColor Green
    Write-Host "Calls made this run: $script:CallsThisRun" -ForegroundColor DarkGray
    exit 0
}

# --- Probe mode --------------------------------------------------
if ($Probe) {
    Write-Host "PROBE: one call with no show_type ($($ProbeCountry.ToUpper()) / $ProbeService)" -ForegroundColor Yellow
    $items = @(Get-TopShows -Country $ProbeCountry -Service $ProbeService -Type '')

    if ($items.Count -eq 0) {
        Write-Warning "No items returned."
        exit 1
    }

    $movies = @($items | Where-Object { $_.showType -eq 'movie' }).Count
    $series = @($items | Where-Object { $_.showType -eq 'series' }).Count

    Write-Host ""
    Write-Host "Items returned: $($items.Count)   (movies: $movies, series: $series)" -ForegroundColor Yellow

    $i = 0
    $items | ForEach-Object {
        $i++
        [pscustomobject]@{ Rank = $i; Type = $_.showType; Title = $_.title }
    } | Format-Table -AutoSize

    Write-Host "Calls made this run: $script:CallsThisRun" -ForegroundColor DarkGray
    exit 0
}

# --- Normal run -----------------------------------------------------------
$discovered = $null
if (-not $serviceMapWasPassed) {
    if (-not (Test-Path $mapFile)) {
        Write-Host "No service-map.json yet - discovering services (1 call)..." -ForegroundColor Cyan
        [void](Update-ServiceMap)
    }
    if (Test-Path $mapFile) {
        $discovered = Get-Content -Path $mapFile -Raw | ConvertFrom-Json
        Write-Host "Using service map: $mapFile" -ForegroundColor DarkGray
    }
    else {
        Write-Warning "Could not build service-map.json - using the built-in service list."
    }
}

$plan = New-Object System.Collections.Generic.List[object]
foreach ($country in $Countries) {
    $key = $country.ToLower()

    if ($discovered) {
        $p = $discovered.PSObject.Properties[$key]
        if (-not $p) {
            Write-Warning "'$country' is not a supported country in the API. Skipping."
            continue
        }
        $services = @($p.Value)
    }
    else {
        $services = @($ServiceMap[$key])
    }

    $services = @($services | Where-Object { $_ -and ($ExcludeServices -notcontains $_) })
    if ($services.Count -eq 0) {
        Write-Warning "No top-10 services for '$country'. Skipping."
        continue
    }

    foreach ($service in $services) {
        $plan.Add([pscustomobject]@{ Country = $key; Service = $service })
    }
}

$types   = if ($SplitByType) { @('movie', 'series') } else { @('') }
$planned = $plan.Count * $types.Count
Write-Host "Planned calls this run: $planned (per-run limit $MaxCallsPerRun)" -ForegroundColor Cyan
if ($planned -gt $MaxCallsPerRun) {
    Write-Warning "This run plans more calls than -MaxCallsPerRun; it will stop at the limit."
}

# --- Fetch ---------------------------------------------------------------------------------
$results = New-Object System.Collections.Generic.List[object]

foreach ($item in $plan) {
    if ($script:StopRun) { break }

    foreach ($type in $types) {
        if ($script:StopRun) { break }

        $items = @(Get-TopShows -Country $item.Country -Service $item.Service -Type $type)

        if ($items.Count -eq 0) {
            Write-Warning "  No results for $($item.Country) / $($item.Service)."
            continue
        }
        if ($type -and $items.Count -lt 10) {
            Write-Warning "  Only $($items.Count) $type items returned for $($item.Country) / $($item.Service) (expected 10)."
        }

        $position     = 0
        $typeCounters = @{}
        foreach ($show in $items) {
            $position++
            $t = $show.showType
            if (-not $typeCounters.ContainsKey($t)) { $typeCounters[$t] = 0 }
            $typeCounters[$t]++

            # Movies use releaseYear, series use firstAirYear
            $year   = if ($show.showType -eq 'movie') { $show.releaseYear } else { $show.firstAirYear }
            $genres = if ($show.genres) { ($show.genres | ForEach-Object { $_.name }) -join ', ' } else { '' }

            $results.Add([pscustomobject]@{
                Date      = $today
                FetchedAt = $fetchedAt
                Country   = $item.Country.ToUpper()
                Service   = $item.Service
                Type      = $t
                Rank      = $position
                TypeRank  = $typeCounters[$t]
                Title     = $show.title
                Year      = $year
                Rating    = $show.rating
                Genres    = $genres
                ImdbId    = $show.imdbId
                TmdbId    = $show.tmdbId
            })
        }

        Start-Sleep -Milliseconds 500
    }
}

# --- Output --------------------------------------------------------------------------------
if ($results.Count -eq 0) {
    Write-Warning "No data returned. Check API key, the service list and quota."
    Write-Host "Calls made this run: $script:CallsThisRun"
    exit 1
}

foreach ($group in ($results | Group-Object Country, Service)) {
    Write-Host ""
    Write-Host "=== $($group.Name) ===" -ForegroundColor Yellow
    $group.Group | Format-Table Rank, Type, TypeRank, Title, Year, Rating -AutoSize
}

$csvPath = Join-Path $histFolder "top-shows-$today.csv"
$results | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8

foreach ($country in @($plan | ForEach-Object { $_.Country } | Select-Object -Unique)) {
    $plannedServices = @($plan | Where-Object { $_.Country -eq $country } | ForEach-Object { $_.Service })
    $rows            = @($results | Where-Object { $_.Country -eq $country.ToUpper() })
    $fetched         = @($rows | ForEach-Object { $_.Service } | Select-Object -Unique)
    if ($fetched.Count -eq 0) { continue }

    $countryFile = Join-Path $topFolder "$country.json"
    $merged = [ordered]@{}

    if (Test-Path $countryFile) {
        try {
            $old = Get-Content -Path $countryFile -Raw | ConvertFrom-Json
            foreach ($p in $old.services.PSObject.Properties) {
                if ($plannedServices -contains $p.Name) { $merged[$p.Name] = $p.Value }
            }
        }
        catch { Write-Warning "Could not read the previous $country.json - starting fresh." }
    }

    foreach ($svc in $fetched) {
        $svcItems = @($rows | Where-Object { $_.Service -eq $svc } | ForEach-Object {
            [ordered]@{
                rank     = $_.Rank
                type     = $_.Type
                typeRank = $_.TypeRank
                title    = $_.Title
                year     = $_.Year
                imdbId   = $_.ImdbId
                tmdbId   = $_.TmdbId
            }
        })
        $merged[$svc] = [ordered]@{ fetchedAt = $fetchedAt; items = $svcItems }
    }

    $ordered = [ordered]@{}
    foreach ($s in $plannedServices) { if ($merged.Contains($s)) { $ordered[$s] = $merged[$s] } }

    $doc = [ordered]@{
        country     = $country
        generatedAt = $fetchedAt
        attribution = $Attribution
        services    = $ordered
    }
    Write-JsonFile -Object $doc -Path $countryFile
}

$index = [ordered]@{ generatedAt = $fetchedAt; attribution = $Attribution; countries = [ordered]@{} }
Get-ChildItem -Path $topFolder -Filter '*.json' |
    Where-Object { $_.Name -ne 'index.json' } |
    Sort-Object Name |
    ForEach-Object {
        try {
            $d = Get-Content -Path $_.FullName -Raw | ConvertFrom-Json
            $index.countries[$d.country] = [ordered]@{
                file      = $_.Name
                updatedAt = $d.generatedAt
                services  = @($d.services.PSObject.Properties.Name)
            }
        }
        catch { Write-Warning "Skipping unreadable $($_.Name) in index." }
    }
Write-JsonFile -Object $index -Path (Join-Path $topFolder 'index.json')

Write-Host ""
Write-Host "Saved $($results.Count) rows to $csvPath" -ForegroundColor Green
Write-Host "JSON files written to $topFolder" -ForegroundColor Green
Write-Host "Calls made this run: $script:CallsThisRun" -ForegroundColor Green
