package com.tonyodev.fetch2.database

import android.arch.persistence.room.*
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.util.*
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.Extras
import com.tonyodev.fetch2core.calculateProgress
import com.tonyodev.fetch2core.getFileUri
import java.util.*


@Entity(tableName = DownloadDatabase.TABLE_NAME,
        indices = [(Index(value = [DownloadDatabase.COLUMN_FILE], unique = true)),
            (Index(value = [DownloadDatabase.COLUMN_GROUP, DownloadDatabase.COLUMN_STATUS], unique = false))])
open class DownloadInfo : Download {

    @PrimaryKey
    @ColumnInfo(name = DownloadDatabase.COLUMN_ID, typeAffinity = ColumnInfo.INTEGER)
    override var id: Int = 0

    @ColumnInfo(name = DownloadDatabase.COLUMN_NAMESPACE, typeAffinity = ColumnInfo.TEXT)
    override var namespace: String = ""

    @ColumnInfo(name = DownloadDatabase.COLUMN_URL, typeAffinity = ColumnInfo.TEXT)
    override var url: String = ""

    @ColumnInfo(name = DownloadDatabase.COLUMN_FILE, typeAffinity = ColumnInfo.TEXT)
    override var file: String = ""

    @ColumnInfo(name = DownloadDatabase.COLUMN_GROUP, typeAffinity = ColumnInfo.INTEGER)
    override var group: Int = 0

    @ColumnInfo(name = DownloadDatabase.COLUMN_PRIORITY, typeAffinity = ColumnInfo.INTEGER)
    override var priority: Priority = defaultPriority

    @ColumnInfo(name = DownloadDatabase.COLUMN_HEADERS, typeAffinity = ColumnInfo.TEXT)
    override var headers: Map<String, String> = mutableMapOf()

    @ColumnInfo(name = DownloadDatabase.COLUMN_DOWNLOADED, typeAffinity = ColumnInfo.INTEGER)
    override var downloaded: Long = 0L

    @ColumnInfo(name = DownloadDatabase.COLUMN_TOTAL, typeAffinity = ColumnInfo.INTEGER)
    override var total: Long = -1L

    @ColumnInfo(name = DownloadDatabase.COLUMN_STATUS, typeAffinity = ColumnInfo.INTEGER)
    override var status: Status = defaultStatus

    @ColumnInfo(name = DownloadDatabase.COLUMN_ERROR, typeAffinity = ColumnInfo.INTEGER)
    override var error: Error = defaultNoError

    @ColumnInfo(name = DownloadDatabase.COLUMN_NETWORK_TYPE, typeAffinity = ColumnInfo.INTEGER)
    override var networkType: NetworkType = defaultNetworkType

    @ColumnInfo(name = DownloadDatabase.COLUMN_CREATED, typeAffinity = ColumnInfo.INTEGER)
    override var created: Long = Calendar.getInstance().timeInMillis

    @ColumnInfo(name = DownloadDatabase.COLUMN_TAG, typeAffinity = ColumnInfo.TEXT)
    override var tag: String? = null

    @ColumnInfo(name = DownloadDatabase.COLUMN_ENQUEUE_ACTION, typeAffinity = ColumnInfo.INTEGER)
    override var enqueueAction: EnqueueAction = EnqueueAction.REPLACE_EXISTING

    @ColumnInfo(name = DownloadDatabase.COLUMN_IDENTIFIER, typeAffinity = ColumnInfo.INTEGER)
    override var identifier: Long = DEFAULT_UNIQUE_IDENTIFIER

    @ColumnInfo(name = DownloadDatabase.COLUMN_DOWNLOAD_ON_ENQUEUE, typeAffinity = ColumnInfo.INTEGER)
    override var downloadOnEnqueue: Boolean = DEFAULT_DOWNLOAD_ON_ENQUEUE

    @ColumnInfo(name = DownloadDatabase.COLUMN_EXTRAS, typeAffinity = ColumnInfo.TEXT)
    override var extras: Extras = Extras.emptyExtras

    @ColumnInfo(name = DownloadDatabase.COLUMN_AUTO_RETRY_MAX_ATTEMPTS, typeAffinity = ColumnInfo.INTEGER)
    override var autoRetryMaxAttempts: Int = DEFAULT_AUTO_RETRY_ATTEMPTS

    @ColumnInfo(name = DownloadDatabase.COLUMN_AUTO_RETRY_ATTEMPTS, typeAffinity = ColumnInfo.INTEGER)
    override var autoRetryAttempts: Int = DEFAULT_AUTO_RETRY_ATTEMPTS

    @Ignore
    override var etaInMilliSeconds: Long = -1L

    @Ignore
    override var downloadedBytesPerSecond: Long = -1L

    override val progress: Int
        get() {
            return calculateProgress(downloaded, total)
        }

    override val fileUri: Uri
        get() {
            return getFileUri(file)
        }

    override val request: Request
        get() {
            val request = Request(url, file)
            request.groupId = group
            request.headers.putAll(headers)
            request.networkType = networkType
            request.priority = priority
            request.enqueueAction = enqueueAction
            request.identifier = identifier
            request.downloadOnEnqueue = downloadOnEnqueue
            request.extras = extras
            request.autoRetryMaxAttempts = autoRetryMaxAttempts
            return request
        }

    override fun copy(): Download {
        return this.toDownloadInfo(DownloadInfo())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DownloadInfo
        if (id != other.id) return false
        if (namespace != other.namespace) return false
        if (url != other.url) return false
        if (file != other.file) return false
        if (group != other.group) return false
        if (priority != other.priority) return false
        if (headers != other.headers) return false
        if (downloaded != other.downloaded) return false
        if (total != other.total) return false
        if (status != other.status) return false
        if (error != other.error) return false
        if (networkType != other.networkType) return false
        if (created != other.created) return false
        if (tag != other.tag) return false
        if (enqueueAction != other.enqueueAction) return false
        if (identifier != other.identifier) return false
        if (downloadOnEnqueue != other.downloadOnEnqueue) return false
        if (extras != other.extras) return false
        if (etaInMilliSeconds != other.etaInMilliSeconds) return false
        if (downloadedBytesPerSecond != other.downloadedBytesPerSecond) return false
        if (autoRetryMaxAttempts != other.autoRetryMaxAttempts) return false
        if (autoRetryAttempts != other.autoRetryAttempts) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + namespace.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + file.hashCode()
        result = 31 * result + group
        result = 31 * result + priority.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + downloaded.hashCode()
        result = 31 * result + total.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + error.hashCode()
        result = 31 * result + networkType.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + (tag?.hashCode() ?: 0)
        result = 31 * result + enqueueAction.hashCode()
        result = 31 * result + identifier.hashCode()
        result = 31 * result + downloadOnEnqueue.hashCode()
        result = 31 * result + extras.hashCode()
        result = 31 * result + etaInMilliSeconds.hashCode()
        result = 31 * result + downloadedBytesPerSecond.hashCode()
        result = 31 * result + autoRetryMaxAttempts.hashCode()
        result = 31 * result + autoRetryAttempts.hashCode()
        return result
    }



    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(namespace)
        dest.writeString(url)
        dest.writeString(file)
        dest.writeInt(group)
        dest.writeInt(priority.value)
        dest.writeSerializable(HashMap(headers))
        dest.writeLong(downloaded)
        dest.writeLong(total)
        dest.writeInt(status.value)
        dest.writeInt(error.value)
        dest.writeInt(networkType.value)
        dest.writeLong(created)
        dest.writeString(tag)
        dest.writeInt(enqueueAction.value)
        dest.writeLong(identifier)
        dest.writeInt(if (downloadOnEnqueue) 1 else 0)
        dest.writeLong(etaInMilliSeconds)
        dest.writeLong(downloadedBytesPerSecond)
        dest.writeSerializable(HashMap(extras.map))
        dest.writeInt(autoRetryMaxAttempts)
        dest.writeInt(autoRetryAttempts)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "DownloadInfo(id=$id, namespace='$namespace', url='$url', file='$file', " +
                "group=$group, priority=$priority, headers=$headers, downloaded=$downloaded," +
                " total=$total, status=$status, error=$error, networkType=$networkType, " +
                "created=$created, tag=$tag, enqueueAction=$enqueueAction, identifier=$identifier," +
                " downloadOnEnqueue=$downloadOnEnqueue, extras=$extras, " +
                "autoRetryMaxAttempts=$autoRetryMaxAttempts, autoRetryAttempts=$autoRetryAttempts," +
                " etaInMilliSeconds=$etaInMilliSeconds, downloadedBytesPerSecond=$downloadedBytesPerSecond)"
    }

    companion object CREATOR : Parcelable.Creator<DownloadInfo> {

        @Suppress("UNCHECKED_CAST")
        override fun createFromParcel(source: Parcel): DownloadInfo {
            val id = source.readInt()
            val namespace = source.readString() ?: ""
            val url = source.readString() ?: ""
            val file = source.readString() ?: ""
            val group = source.readInt()
            val priority = Priority.valueOf(source.readInt())
            val headers = source.readSerializable() as Map<String, String>
            val downloaded = source.readLong()
            val total = source.readLong()
            val status = Status.valueOf(source.readInt())
            val error = Error.valueOf(source.readInt())
            val networkType = NetworkType.valueOf(source.readInt())
            val created = source.readLong()
            val tag = source.readString()
            val enqueueAction = EnqueueAction.valueOf(source.readInt())
            val identifier = source.readLong()
            val downloadOnEnqueue = source.readInt() == 1
            val etaInMilliSeconds = source.readLong()
            val downloadedBytesPerSecond = source.readLong()
            val extras = source.readSerializable() as Map<String, String>
            val autoRetryMaxAttempts = source.readInt()
            val autoRetryAttempts = source.readInt()

            val downloadInfo = DownloadInfo()
            downloadInfo.id = id
            downloadInfo.namespace = namespace
            downloadInfo.url = url
            downloadInfo.file = file
            downloadInfo.group = group
            downloadInfo.priority = priority
            downloadInfo.headers = headers
            downloadInfo.downloaded = downloaded
            downloadInfo.total = total
            downloadInfo.status = status
            downloadInfo.error = error
            downloadInfo.networkType = networkType
            downloadInfo.created = created
            downloadInfo.tag = tag
            downloadInfo.enqueueAction = enqueueAction
            downloadInfo.identifier = identifier
            downloadInfo.downloadOnEnqueue = downloadOnEnqueue
            downloadInfo.etaInMilliSeconds = etaInMilliSeconds
            downloadInfo.downloadedBytesPerSecond = downloadedBytesPerSecond
            downloadInfo.extras = Extras(extras)
            downloadInfo.autoRetryMaxAttempts = autoRetryMaxAttempts
            downloadInfo.autoRetryAttempts = autoRetryAttempts
            return downloadInfo
        }

        override fun newArray(size: Int): Array<DownloadInfo?> {
            return arrayOfNulls(size)
        }

    }

}