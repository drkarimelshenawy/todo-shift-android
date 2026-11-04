package com.todoshift.app.cloud

import java.time.Instant
import java.time.ZoneId
import java.io.InputStream

/** Ports only: SDK adapters and OAuth configuration are deliberately not installed/enabled. */
sealed class CloudResult<out T> {
    data class Success<T>(val value: T) : CloudResult<T>()
    data class Unavailable(val reason: String) : CloudResult<Nothing>()
    data class Failure(val code: String, val userMessage: String) : CloudResult<Nothing>()
    object Cancelled : CloudResult<Nothing>()
}
data class CloudAccount(val subject: String, val email: String?, val displayName: String?)
data class DriveAuthorization(val accountSubject: String, val grantedScopes: Set<String>, val expiresAt: Instant?)
data class CloudBackupMetadata(val id: String, val accountSubject: String, val createdAt: Instant, val bytes: Long, val checksum: String, val formatVersion: Int)
interface GoogleAuthentication { val configured: Boolean; suspend fun signIn(): CloudResult<CloudAccount>; suspend fun signOut(): CloudResult<Unit> }
interface GoogleAuthorization { suspend fun authorize(account: CloudAccount, scopes: Set<String>): CloudResult<DriveAuthorization>; suspend fun revoke(account: CloudAccount): CloudResult<Unit> }
interface DriveBackup {
    suspend fun upload(auth: DriveAuthorization, metadata: CloudBackupMetadata, data: InputStream): CloudResult<CloudBackupMetadata>
    suspend fun list(auth: DriveAuthorization): CloudResult<List<CloudBackupMetadata>>
    suspend fun download(auth: DriveAuthorization, id: String): CloudResult<InputStream>
    suspend fun delete(auth: DriveAuthorization, id: String): CloudResult<Unit>
}
enum class BackupFrequency { DISABLED, DAILY, WEEKLY, MONTHLY }
data class BackupSchedule(val frequency: BackupFrequency = BackupFrequency.DISABLED, val wifiOnly: Boolean = true)
interface BackupScheduling { suspend fun configure(schedule: BackupSchedule): CloudResult<Unit>; suspend fun cancel(): CloudResult<Unit> }
/** True multi-device sync requires record revisions, tombstones and conflict resolution; it is not snapshot restore. */
interface CloudSyncFuture { suspend fun inspectRemoteRevisions(): CloudResult<Unit> }
object CloudScope { const val DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata" }
object BackupSchedulePlanner {
    fun nextDue(lastSuccess: Instant, schedule: BackupSchedule, zone: ZoneId): Instant? = when(schedule.frequency) {
        BackupFrequency.DISABLED -> null
        BackupFrequency.DAILY -> lastSuccess.atZone(zone).plusDays(1).toInstant()
        BackupFrequency.WEEKLY -> lastSuccess.atZone(zone).plusWeeks(1).toInstant()
        BackupFrequency.MONTHLY -> lastSuccess.atZone(zone).plusMonths(1).toInstant()
    }
}
object DisabledCloud : GoogleAuthentication, GoogleAuthorization, DriveBackup, BackupScheduling, CloudSyncFuture {
    override val configured = false
    private fun unavailable() = CloudResult.Unavailable("Google SDK adapters, OAuth clients and end-to-end validation are not configured")
    override suspend fun signIn(): CloudResult<CloudAccount> = unavailable()
    override suspend fun signOut(): CloudResult<Unit> = unavailable()
    override suspend fun authorize(account: CloudAccount, scopes: Set<String>): CloudResult<DriveAuthorization> = unavailable()
    override suspend fun revoke(account: CloudAccount): CloudResult<Unit> = unavailable()
    override suspend fun upload(auth: DriveAuthorization, metadata: CloudBackupMetadata, data: InputStream): CloudResult<CloudBackupMetadata> = unavailable()
    override suspend fun list(auth: DriveAuthorization): CloudResult<List<CloudBackupMetadata>> = unavailable()
    override suspend fun download(auth: DriveAuthorization, id: String): CloudResult<InputStream> = unavailable()
    override suspend fun delete(auth: DriveAuthorization, id: String): CloudResult<Unit> = unavailable()
    override suspend fun configure(schedule: BackupSchedule): CloudResult<Unit> = unavailable()
    override suspend fun cancel(): CloudResult<Unit> = CloudResult.Success(Unit)
    override suspend fun inspectRemoteRevisions(): CloudResult<Unit> = unavailable()
}
