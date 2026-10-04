package com.floently.read

import android.content.Context
import com.floently.shared.billing.FloentlyAccessStatus
import com.floently.shared.billing.accessStatusFromJson
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ReadAccessLeaseRecord(
    val status: FloentlyAccessStatus,
    val verifiedAtMs: Long
)

object ReadAccessLeaseStore {
    private const val SCHEMA_VERSION = 1
    private const val DIRECTORY_NAME =
        "read-access-leases"
    private const val MAXIMUM_AGE_MS =
        7L * 24L * 60L * 60L * 1_000L

    suspend fun save(
        context: Context,
        status: FloentlyAccessStatus,
        accountIdentity: String,
        verifiedAtMs: Long =
            System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        if (!isGranted(status)) {
            removeFile(
                context = context,
                accountIdentity = accountIdentity
            )
            return@withContext
        }

        val directory = directory(context)
        check(
            directory.exists()
                || directory.mkdirs()
        ) {
            "Could not create local access storage."
        }

        val root = JSONObject()
            .put(
                "schemaVersion",
                SCHEMA_VERSION
            )
            .put(
                "verifiedAtMs",
                verifiedAtMs
            )
            .put(
                "status",
                encodeStatus(status)
            )

        val target = leaseFile(
            context = context,
            accountIdentity = accountIdentity
        )
        val temporary = File(
            directory,
            target.name + ".tmp"
        )

        try {
            temporary.writeText(
                root.toString()
            )
            if (
                target.exists()
                && !target.delete()
            ) {
                error(
                    "Could not replace local access storage."
                )
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(
                    target = target,
                    overwrite = true
                )
                temporary.delete()
            }
        } finally {
            if (temporary.exists()) {
                temporary.delete()
            }
        }
    }

    suspend fun validGrantedLease(
        context: Context,
        accountIdentity: String,
        nowMs: Long =
            System.currentTimeMillis()
    ): ReadAccessLeaseRecord? =
        withContext(Dispatchers.IO) {
            val file = leaseFile(
                context = context,
                accountIdentity = accountIdentity
            )
            if (!file.isFile) {
                return@withContext null
            }

            val root = runCatching {
                JSONObject(
                    file.readText()
                )
            }.getOrNull()
                ?: return@withContext null

            if (
                root.optInt(
                    "schemaVersion",
                    0
                ) != SCHEMA_VERSION
            ) {
                return@withContext null
            }

            val verifiedAtMs =
                root.optLong(
                    "verifiedAtMs",
                    0L
                )
            val age =
                nowMs - verifiedAtMs
            if (
                verifiedAtMs <= 0L
                || age < -5L * 60L * 1_000L
                || age > MAXIMUM_AGE_MS
            ) {
                file.delete()
                return@withContext null
            }

            val statusJson =
                root.optJSONObject("status")
                    ?: return@withContext null
            val status =
                runCatching {
                    accessStatusFromJson(
                        statusJson
                    )
                }.getOrNull()
                    ?: return@withContext null

            if (!isGranted(status)) {
                file.delete()
                return@withContext null
            }

            ReadAccessLeaseRecord(
                status = status,
                verifiedAtMs = verifiedAtMs
            )
        }

    suspend fun remove(
        context: Context,
        accountIdentity: String
    ) = withContext(Dispatchers.IO) {
        removeFile(
            context = context,
            accountIdentity = accountIdentity
        )
        Unit
    }

    suspend fun clearAll(
        context: Context
    ) = withContext(Dispatchers.IO) {
        directory(context)
            .deleteRecursively()
        Unit
    }

    private fun encodeStatus(
        status: FloentlyAccessStatus
    ): JSONObject =
        JSONObject()
            .put(
                "billingTier",
                status.billingTier
            )
            .put(
                "subscriptionStatus",
                status.subscriptionStatus
            )
            .put(
                "ykiAccess",
                status.ykiAccess
            )
            .put(
                "professionalAccess",
                status.professionalAccess
            )
            .put(
                "combinedAccess",
                status.combinedAccess
            )
            .put(
                "readAccess",
                status.readAccess
            )
            .put(
                "createAccess",
                status.createAccess
            )
            .put(
                "isInternalAllAccess",
                status.isInternalAllAccess
            )
            .put(
                "trialAlreadyUsed",
                status.trialAlreadyUsed
            )
            .put(
                "canStartTrial",
                status.canStartTrial
            )
            .put(
                "cancelAtPeriodEnd",
                status.cancelAtPeriodEnd
            )
            .put(
                "hasPaymentIssue",
                status.hasPaymentIssue
            )
            .put(
                "paymentIssueMessage",
                status.paymentIssueMessage
            )

    private fun isGranted(
        status: FloentlyAccessStatus
    ): Boolean =
        status.readAccess
            || status.isInternalAllAccess

    private fun removeFile(
        context: Context,
        accountIdentity: String
    ) {
        leaseFile(
            context = context,
            accountIdentity = accountIdentity
        ).delete()
    }

    private fun directory(
        context: Context
    ): File = File(
        context.applicationContext.filesDir,
        DIRECTORY_NAME
    )

    private fun leaseFile(
        context: Context,
        accountIdentity: String
    ): File = File(
        directory(context),
        storageKey(accountIdentity)
            + ".json"
    )

    private fun storageKey(
        value: String
    ): String =
        MessageDigest.getInstance("SHA-256")
            .digest(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )
            .joinToString("") { byte ->
                "%02x".format(
                    byte.toInt() and 0xff
                )
            }
}
