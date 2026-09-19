# Poshan — Appwrite schema provisioning
#
# Creates the `poshan_db` database and the collections the Android app syncs to.
# Credentials are read from environment variables so nothing secret is committed.
#
# Required API key scopes: databases.write, collections.read, collections.write
#
# Usage (PowerShell):
#   $env:APPWRITE_ENDPOINT   = "https://<your-appwrite-host>/v1"
#   $env:APPWRITE_PROJECT_ID = "<your-project-id>"
#   $env:APPWRITE_API_KEY    = "<your key>"
#   pwsh -File appwrite/schema.ps1

$ErrorActionPreference = "Continue"

$base = $env:APPWRITE_ENDPOINT
$proj = $env:APPWRITE_PROJECT_ID
$key  = $env:APPWRITE_API_KEY
$db   = if ($env:APPWRITE_DATABASE_ID) { $env:APPWRITE_DATABASE_ID } else { "poshan_db" }

if (-not $base -or -not $proj -or -not $key) {
    Write-Error "Set APPWRITE_ENDPOINT, APPWRITE_PROJECT_ID and APPWRITE_API_KEY first."
    exit 1
}

$h = @{ "X-Appwrite-Project" = $proj; "X-Appwrite-Key" = $key; "Content-Type" = "application/json" }

function Call($method, $path, $body) {
    try {
        if ($null -ne $body) {
            return Invoke-RestMethod -Uri "$base$path" -Method $method -Headers $h `
                -Body ($body | ConvertTo-Json -Depth 6 -Compress)
        }
        return Invoke-RestMethod -Uri "$base$path" -Method $method -Headers $h
    } catch {
        $code = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
        if ($code -eq 409) { return "EXISTS" }
        return "ERR($code): $($_.ErrorDetails.Message)"
    }
}

Write-Output ("database: " + (Call "POST" "/databases" @{ databaseId = $db; name = "Poshan" }))

# Collection-level permissions.
#
#   create("users")  — any signed-in user (anonymous sessions included) may add documents.
#   *("label:admin") — staff with the `admin` label can read/update/delete everything.
#
# The admin grant MUST live here, at collection level, and not in the per-document
# permissions the Android client sends. Appwrite only lets a client SDK grant roles that the
# calling session actually holds (`any`, `users`, `user:<id>`); a document created with
# `read("label:admin")` is rejected with:
#     401 user_unauthorized — Permissions must be one of: (any, users, user:<id>, ...)
# which used to make every single sync write fail.
$collectionPermissions = @(
    'create("users")',
    'read("label:admin")',
    'update("label:admin")',
    'delete("label:admin")'
)

function New-Collection($id, $name) {
    $r = Call "POST" "/databases/$db/collections" @{
        collectionId     = $id
        name             = $name
        permissions      = $collectionPermissions
        documentSecurity = $true
    }
    # Re-apply on every run so existing collections pick up permission changes too.
    $u = Call "PUT" "/databases/$db/collections/$id" @{
        name             = $name
        permissions      = $collectionPermissions
        documentSecurity = $true
        enabled          = $true
    }
    Write-Output "collection $id : create=$r perms=$(if ($u -is [string]) { $u } else { 'applied' })"
}

function Attr($col, $type, $body) {
    $r = Call "POST" "/databases/$db/collections/$col/attributes/$type" $body
    if ($r -notlike "EXISTS*") { Write-Output ("  " + $body.key + " : " + $r) }
}

New-Collection "profiles" "Profiles"
New-Collection "children" "Children"
New-Collection "assessments" "Assessments"
New-Collection "growth_records" "Growth Records"
New-Collection "attention_records" "Attention Records"
New-Collection "vitamin_d_records" "Vitamin D Records"

Write-Output "--- profiles ---"
Attr "profiles" "string"  @{ key="user_id"; size=64; required=$true }
Attr "profiles" "string"  @{ key="role"; size=32; required=$true }
Attr "profiles" "string"  @{ key="display_name"; size=128; required=$false; default="" }
Attr "profiles" "string"  @{ key="phone"; size=32; required=$false; default="" }
Attr "profiles" "string"  @{ key="relationship"; size=64; required=$false; default="" }
Attr "profiles" "string"  @{ key="location"; size=256; required=$false; default="" }
Attr "profiles" "string"  @{ key="anganwadi_name"; size=128; required=$false; default="" }
Attr "profiles" "string"  @{ key="anganwadi_code"; size=16; required=$false; default="" }
Attr "profiles" "string"  @{ key="facility_name"; size=128; required=$false; default="" }
Attr "profiles" "string"  @{ key="hfr_id"; size=64; required=$false; default="" }
Attr "profiles" "string"  @{ key="employee_id"; size=64; required=$false; default="" }
Attr "profiles" "boolean" @{ key="is_guest"; required=$false; default=$false }

Write-Output "--- children ---"
Attr "children" "string"  @{ key="child_id"; size=64; required=$true }
Attr "children" "string"  @{ key="name"; size=128; required=$true }
Attr "children" "integer" @{ key="date_of_birth_epoch_day"; required=$true }
Attr "children" "enum"    @{ key="gender"; elements=@("MALE","FEMALE","OTHER"); required=$true }
Attr "children" "boolean" @{ key="born_premature"; required=$false; default=$false }
Attr "children" "integer" @{ key="gestational_age_weeks"; required=$false }
Attr "children" "string"  @{ key="parent_name"; size=128; required=$false; default="" }
Attr "children" "string"  @{ key="parent_mobile"; size=32; required=$false; default="" }
Attr "children" "string"  @{ key="village"; size=128; required=$false; default="" }
Attr "children" "string"  @{ key="created_by_role"; size=32; required=$false; default="PARENT" }
Attr "children" "string"  @{ key="owner_id"; size=64; required=$false; default="" }

Write-Output "--- assessments ---"
Attr "assessments" "string"  @{ key="assessment_id"; size=64; required=$true }
Attr "assessments" "string"  @{ key="child_id"; size=64; required=$true }
Attr "assessments" "integer" @{ key="assessment_date"; required=$true }
Attr "assessments" "float"   @{ key="age_months"; required=$false }
Attr "assessments" "float"   @{ key="corrected_age_months"; required=$false }
Attr "assessments" "string"  @{ key="answers_json"; size=16384; required=$false; default="[]" }
Attr "assessments" "string"  @{ key="growth_json"; size=2048; required=$false; default="" }
Attr "assessments" "string"  @{ key="nutrition_json"; size=4096; required=$false; default="" }
Attr "assessments" "string"  @{ key="attention_json"; size=4096; required=$false; default="" }
Attr "assessments" "string"  @{ key="vitamin_d_json"; size=4096; required=$false; default="" }
Attr "assessments" "string"  @{ key="given_vaccines_json"; size=4096; required=$false; default="[]" }
Attr "assessments" "string"  @{ key="category_results_json"; size=8192; required=$false; default="[]" }
Attr "assessments" "string"  @{ key="composite_json"; size=4096; required=$false; default="" }
Attr "assessments" "float"   @{ key="dq"; required=$false }
Attr "assessments" "string"  @{ key="status"; size=16; required=$false; default="DRAFT" }
Attr "assessments" "string"  @{ key="assessor_role"; size=32; required=$false; default="PARENT" }
Attr "assessments" "string"  @{ key="owner_id"; size=64; required=$false; default="" }

Write-Output "--- growth_records ---"
Attr "growth_records" "string"  @{ key="child_id"; size=64; required=$true }
Attr "growth_records" "integer" @{ key="record_date"; required=$true }
Attr "growth_records" "float"   @{ key="weight_kg"; required=$false }
Attr "growth_records" "float"   @{ key="height_cm"; required=$false }
# MUAC replaced head circumference as the wasting indicator; the old attribute is kept
# so documents written before the change still read back, but nothing writes to it now.
Attr "growth_records" "float"   @{ key="head_circumference_cm"; required=$false }
Attr "growth_records" "float"   @{ key="muac_mm"; required=$false }
Attr "growth_records" "string"  @{ key="owner_id"; size=64; required=$false; default="" }

Write-Output "--- attention_records ---"
Attr "attention_records" "string"  @{ key="child_id"; size=64; required=$true }
Attr "attention_records" "string"  @{ key="task_type"; size=32; required=$false; default="PREFERENTIAL_LOOKING" }
Attr "attention_records" "float"   @{ key="valid_frame_ratio"; required=$false }
Attr "attention_records" "float"   @{ key="on_screen_ratio"; required=$false }
Attr "attention_records" "float"   @{ key="social_attention_ratio"; required=$false }
Attr "attention_records" "float"   @{ key="blink_rate"; required=$false }
Attr "attention_records" "float"   @{ key="engagement_index"; required=$false }
Attr "attention_records" "string"  @{ key="band"; size=16; required=$false; default="ON_TRACK" }
Attr "attention_records" "boolean" @{ key="inconclusive"; required=$false; default=$true }
Attr "attention_records" "integer" @{ key="record_date"; required=$false }
Attr "attention_records" "string"  @{ key="owner_id"; size=64; required=$false; default="" }

Write-Output "--- vitamin_d_records ---"
Attr "vitamin_d_records" "string"  @{ key="child_id"; size=64; required=$true }
Attr "vitamin_d_records" "integer" @{ key="score"; required=$false }
Attr "vitamin_d_records" "string"  @{ key="band"; size=16; required=$false; default="ON_TRACK" }
Attr "vitamin_d_records" "string"  @{ key="factors_json"; size=4096; required=$false; default="[]" }
Attr "vitamin_d_records" "integer" @{ key="record_date"; required=$false }
Attr "vitamin_d_records" "string"  @{ key="owner_id"; size=64; required=$false; default="" }

Write-Output "DONE"
