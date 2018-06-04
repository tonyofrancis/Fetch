package com.tonyodev.fetch2.database

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.util.*


@Entity(tableName = DownloadDatabase.TABLE_NAME,
        indices = [(Index(value = [DownloadDatabase.COLUMN_FILE], unique = true)),
            (Index(value = [DownloadDatabase.COLUMN_GROUP, DownloadDatabase.COLUMN_STATUS], unique = false))])
class DownloadInfo : Download {

    @PrimaryKey
    @ColumnInfo(name = DownloadDatabase.COLUMN_ID)
    override var id: Int = 0

    @ColumnInfo(name = DownloadDatabase.COLUMN_NAMESPACE)
    override var namespace: String = ""

    @ColumnInfo(name = DownloadDatabase.COLUMN_URL)
    override var url: String = ""

    @ColumnInfo(name = DownloadDatabase.COLUMN_FILE)
    override var file: String = ""

    @ColumnInfo(name = DownloadDatabase.COLUMN_GROUP)
    override var group: Int = 0

    @ColumnInfo(name = DownloadDatabase.COLUMN_PRIORITY)
    override var priority: Priority = defaultPriority

    @ColumnInfo(name = DownloadDatabase.COLUMN_HEADERS)
    override var headers: Map<String, String> = defaultEmptyHeaderMap

    @ColumnInfo(name = DownloadDatabase.COLUMN_DOWNLOADED)
    override var downloaded: Long = 0L

    @ColumnInfo(name = DownloadDatabase.COLUMN_TOTAL)
    override var total: Long = -1L

    @ColumnInfo(name = DownloadDatabase.COLUMN_STATUS)
    override var status: Status = defaultStatus

    @ColumnInfo(name = DownloadDatabase.COLUMN_ERROR)
    override var error: Error = defaultNoError

    @ColumnInfo(name = DownloadDatabase.COLUMN_NETWORK_TYPE)
    override var networkType: NetworkType = defaultNetworkType

    @ColumnInfo(name = DownloadDatabase.COLUMN_CREATED)
    override var created: Long = System.nanoTime()

    @ColumnInfo(name = DownloadDatabase.COLUMN_TAG)
    override var tag: String? = null

    @ColumnInfo(name = DownloadDatabase.COLUMN_ENQUEUE_ACTION, typeAffinity = ColumnInfo.INTEGER)
    override var enqueueAction: EnqueueAction = EnqueueAction.REPLACE_EXISTING

    @ColumnInfo(name = DownloadDatabase.COLUMN_IDENTIFIER)
    override var identifier: Long = DEFAULT_UNIQUE_IDENTIFIER

    override val progress: Int
        get() {
            return calculateProgress(downloaded, total)
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
            return request
        }

    override fun copy(): Download {
        return this.toDownloadInfo()
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
        return result
    }

    override fun toString(): String {
        return "DownloadInfo(id=$id, namespace='$namespace', url='$url', file='$file', group=$group," +
                " priority=$priority, headers=$headers, downloaded=$downloaded, total=$total, status=$status," +
                " error=$error, networkType=$networkType, created=$created, tag=$tag, " +
                "enqueueAction=$enqueueAction, identifier=$identifier)"
    }


}