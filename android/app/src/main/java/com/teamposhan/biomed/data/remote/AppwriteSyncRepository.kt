package com.teamposhan.biomed.data.remote

import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.data.model.ChildProfile
import com.teamposhan.biomed.data.model.UserProfile
import io.appwrite.Permission
import io.appwrite.Role
import io.appwrite.exceptions.AppwriteException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Pushes locally collected screening data to Appwrite. The app is offline-first, so this
 * runs opportunistically: if there is no network, no session, or the collections are not
 * provisioned, it reports exactly why and the local data stays intact.
 *
 * Permission model: each document is private to its owner. Note that a client SDK may only
 * grant roles it actually holds — `user:<id>`, `users`, `any`. Granting `label:admin` from
 * the device is rejected by Appwrite with 401 `user_unauthorized`, which used to fail every
 * single write. Admin-wide read access is therefore granted at the *collection* level by
 * appwrite/schema.ps1 instead of per document.
 */
class AppwriteSyncRepository(private val auth: AuthRepository) {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private val stringListSerializer = ListSerializer(String.serializer())

    data class SyncReport(
        val attempted: Int = 0,
        val uploaded: Int = 0,
        val failed: Int = 0,
        val message: String = "",
        /** True when a later attempt has a realistic chance of succeeding. */
        val retryable: Boolean = false,
    ) {
        val ok: Boolean get() = failed == 0 && uploaded > 0
    }

    /** Result of a single document write. */
    private sealed interface Outcome {
        object Ok : Outcome
        data class Fail(val what: String, val why: String, val retryable: Boolean) : Outcome
    }

    suspend fun syncAll(
        profile: UserProfile?,
        children: List<ChildProfile>,
        sessions: List<AssessmentSession>,
    ): SyncReport = withContext(Dispatchers.IO) {
        if (!AppwriteService.isConfigured) {
            return@withContext SyncReport(message = "Appwrite not configured")
        }

        val ownerId = try {
            auth.ensureRemoteSession()
        } catch (e: Throwable) {
            val why = e.message?.takeIf { it.isNotBlank() } ?: (e::class.simpleName ?: "unknown error")
            return@withContext SyncReport(
                failed = 1,
                message = "Sign-in failed: $why",
                retryable = isTransient(e),
            )
        } ?: return@withContext SyncReport(message = "Appwrite not configured")

        val perms = ownerPermissions(ownerId)
        val outcomes = mutableListOf<Outcome>()

        // Every record is attempted independently: one bad document never stops the rest.
        profile?.let { outcomes += uploadProfile(it, perms) }
        children.forEach { child -> outcomes += uploadChild(child, ownerId, perms) }
        sessions.forEach { session -> outcomes += uploadSession(session, ownerId, perms) }

        val failures = outcomes.filterIsInstance<Outcome.Fail>()
        val uploaded = outcomes.size - failures.size

        SyncReport(
            attempted = outcomes.size,
            uploaded = uploaded,
            failed = failures.size,
            message = when {
                outcomes.isEmpty() -> "Nothing to sync"
                failures.isEmpty() -> "Synced $uploaded record(s)"
                // Show the actual server reason, not "n of m failed".
                else -> "${failures.size} of ${outcomes.size} failed — " +
                    "${failures.first().what}: ${failures.first().why}"
            },
            // Retry only when at least one failure could plausibly clear itself.
            retryable = failures.any { it.retryable },
        )
    }

    private fun ownerPermissions(ownerId: String): List<String> = listOf(
        Permission.read(Role.user(ownerId)),
        Permission.update(Role.user(ownerId)),
        Permission.delete(Role.user(ownerId)),
    )

    /**
     * createDocument, falling back to updateDocument when the document already exists so
     * repeated syncs are idempotent. Any other failure is reported verbatim.
     */
    private suspend fun upsert(
        collection: String,
        documentId: String,
        data: Map<String, Any?>,
        perms: List<String>,
    ): Outcome {
        val clean = data.filterValues { it != null }
        val what = "$collection/$documentId"
        try {
            AppwriteService.databases.createDocument(
                databaseId = AppwriteService.databaseId,
                collectionId = collection,
                documentId = documentId,
                data = clean,
                permissions = perms,
            )
            return Outcome.Ok
        } catch (create: AppwriteException) {
            val alreadyExists = create.code == 409 ||
                create.type == "document_already_exists"
            if (!alreadyExists) {
                return Outcome.Fail(what, describe(create), isTransient(create))
            }
            return try {
                AppwriteService.databases.updateDocument(
                    databaseId = AppwriteService.databaseId,
                    collectionId = collection,
                    documentId = documentId,
                    data = clean,
                )
                Outcome.Ok
            } catch (update: AppwriteException) {
                Outcome.Fail(what, describe(update), isTransient(update))
            } catch (update: Throwable) {
                Outcome.Fail(what, update.message ?: "network error", isTransient(update))
            }
        } catch (create: Throwable) {
            return Outcome.Fail(what, create.message ?: "network error", isTransient(create))
        }
    }

    /** Human-readable, actionable text for an Appwrite failure. */
    private fun describe(e: AppwriteException): String {
        val msg = e.message?.takeIf { it.isNotBlank() } ?: "unknown error"
        val hint = when {
            e.type == "collection_not_found" || e.type == "database_not_found" ->
                " — run appwrite/schema.ps1 to provision the database."
            e.type == "user_unauthorized" ->
                " — the collection does not grant create to role \"users\"; run appwrite/schema.ps1."
            e.type == "document_invalid_structure" || e.code == 400 ->
                " — the schema is out of date; run appwrite/schema.ps1."
            else -> ""
        }
        return "$msg (code ${e.code})$hint"
    }

    /** Transient = worth retrying later; a schema or permission error is not. */
    private fun isTransient(e: Throwable): Boolean = when (e) {
        is AppwriteException -> e.code == null || e.code == 0 || e.code == 429 || (e.code ?: 0) >= 500
        is IOException -> true
        else -> e.cause?.let { it is IOException } ?: false
    }

    private suspend fun uploadProfile(profile: UserProfile, perms: List<String>): Outcome = upsert(
        collection = AppwriteService.Collections.PROFILES,
        documentId = profile.id,
        data = mapOf(
            "user_id" to profile.id,
            "role" to profile.role.name,
            "display_name" to profile.displayName,
            "phone" to profile.phone,
            "relationship" to profile.relationship,
            "location" to profile.location,
            "anganwadi_name" to profile.anganwadiName,
            "anganwadi_code" to profile.anganwadiCode,
            "facility_name" to profile.facilityName,
            "hfr_id" to profile.hfrId,
            "employee_id" to profile.employeeId,
            "is_guest" to profile.isGuest,
        ),
        perms = perms,
    )

    private suspend fun uploadChild(child: ChildProfile, ownerId: String, perms: List<String>): Outcome = upsert(
        collection = AppwriteService.Collections.CHILDREN,
        documentId = child.id,
        data = mapOf(
            "child_id" to child.id,
            "name" to child.name,
            "date_of_birth_epoch_day" to child.dateOfBirthEpochDay,
            "gender" to child.sex.name,
            "born_premature" to child.bornPremature,
            "gestational_age_weeks" to child.gestationalAgeWeeks,
            "parent_name" to child.parentName,
            "parent_mobile" to child.parentMobile,
            "village" to child.village,
            "created_by_role" to child.createdByRole.name,
            "owner_id" to ownerId,
        ),
        perms = perms,
    )

    /** One assessment can produce up to four documents; each is reported separately. */
    private suspend fun uploadSession(
        session: AssessmentSession,
        ownerId: String,
        perms: List<String>,
    ): List<Outcome> {
        val outcomes = mutableListOf<Outcome>()

        outcomes += upsert(
            collection = AppwriteService.Collections.ASSESSMENTS,
            documentId = session.id,
            data = mapOf(
                "assessment_id" to session.id,
                "child_id" to session.childId,
                "assessment_date" to session.startedAt,
                "age_months" to session.ageMonthsAtAssessment,
                "corrected_age_months" to session.correctedAgeMonths,
                "answers_json" to json.encodeToString(
                    ListSerializer(com.teamposhan.biomed.data.model.AssessmentAnswer.serializer()),
                    session.answers,
                ),
                "growth_json" to session.growth?.let { json.encodeToString(com.teamposhan.biomed.data.model.GrowthMeasurement.serializer(), it) },
                "nutrition_json" to session.nutrition?.let { json.encodeToString(com.teamposhan.biomed.data.model.NutritionData.serializer(), it) },
                "attention_json" to session.attention?.let { json.encodeToString(com.teamposhan.biomed.data.model.AttentionResult.serializer(), it) },
                "vitamin_d_json" to session.vitaminD?.let { json.encodeToString(com.teamposhan.biomed.data.model.VitaminDResult.serializer(), it) },
                "given_vaccines_json" to json.encodeToString(stringListSerializer, session.givenVaccines),
                "category_results_json" to json.encodeToString(
                    ListSerializer(com.teamposhan.biomed.data.model.CategoryResult.serializer()),
                    session.categoryResults,
                ),
                "composite_json" to session.composite?.let { json.encodeToString(com.teamposhan.biomed.data.model.CompositeRisk.serializer(), it) },
                "dq" to session.dq?.dq,
                "status" to session.status.name,
                "assessor_role" to session.assessorRole.name,
                "owner_id" to ownerId,
            ),
            perms = perms,
        )

        session.attention?.let { a ->
            outcomes += upsert(
                collection = AppwriteService.Collections.ATTENTION,
                documentId = subDocumentId(session.id, "attn"),
                data = mapOf(
                    "child_id" to session.childId,
                    "task_type" to a.taskType.name,
                    "valid_frame_ratio" to a.validFrameRatio,
                    "on_screen_ratio" to a.onScreenRatio,
                    "social_attention_ratio" to a.socialAttentionRatio,
                    "blink_rate" to a.blinkRatePerMin,
                    "engagement_index" to a.engagementIndex,
                    "band" to a.band.name,
                    "inconclusive" to a.inconclusive,
                    "record_date" to session.startedAt,
                    "owner_id" to ownerId,
                ),
                perms = perms,
            )
        }
        session.vitaminD?.let { v ->
            outcomes += upsert(
                collection = AppwriteService.Collections.VITAMIN_D,
                documentId = subDocumentId(session.id, "vitd"),
                data = mapOf(
                    "child_id" to session.childId,
                    "score" to v.score,
                    "band" to v.band.name,
                    "factors_json" to json.encodeToString(stringListSerializer, v.factors),
                    "record_date" to session.startedAt,
                    "owner_id" to ownerId,
                ),
                perms = perms,
            )
        }
        session.growth?.let { g ->
            outcomes += upsert(
                collection = AppwriteService.Collections.GROWTH,
                documentId = subDocumentId(session.id, "growth"),
                data = mapOf(
                    "child_id" to session.childId,
                    "record_date" to g.dateMillis,
                    "weight_kg" to g.weightKg,
                    "height_cm" to g.heightCm,
                    // MUAC is millimetres; it was previously written into the
                    // head_circumference_cm column, which both misnamed it and implied
                    // the wrong unit.
                    "muac_mm" to g.muacMm,
                    "owner_id" to ownerId,
                ),
                perms = perms,
            )
        }
        return outcomes
    }

    /**
     * Appwrite document ids are limited to 36 characters. Session ids are 36-char UUIDs, so
     * appending a suffix would overflow; keep the suffix and trim the uuid instead. The
     * result is stable for a given session, which keeps the upsert idempotent.
     */
    private fun subDocumentId(sessionId: String, suffix: String): String {
        val keep = MAX_DOCUMENT_ID - suffix.length - 1
        val head = sessionId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        return (if (head.length > keep) head.take(keep) else head) + "_" + suffix
    }

    private companion object {
        const val MAX_DOCUMENT_ID = 36
    }
}
