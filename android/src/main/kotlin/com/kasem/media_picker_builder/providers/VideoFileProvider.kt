package com.kasem.media_picker_builder.providers

import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import com.kasem.media_picker_builder.model.Album
import com.kasem.media_picker_builder.model.MediaFile
import java.text.SimpleDateFormat
import java.util.*

object VideoFileProvider {

    fun getVideoMediaFile(
            context: Context,
            fileId: Long
    ): MediaFile? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cursor = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(
                            MediaStore.Video.Media._ID,
                            MediaStore.Video.Media.DATE_ADDED,
                            MediaStore.Video.Media.DATA,
                            MediaStore.Video.Media.MIME_TYPE,
                            MediaStore.Video.Media.BUCKET_ID,
                            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                            MediaStore.Video.Media.DURATION,
                            MediaStore.Video.Media.ORIENTATION,
                            MediaStore.Video.Media.DATE_TAKEN),
                    "${MediaStore.Video.Media._ID} = $fileId",
                    null,
                    null)

            return if (cursor != null && cursor.moveToFirst()) {
                cursorToMediaFileSubsequent29(cursor, startDate = null, endDate = null)
            } else {
                null
            }
        } else {
            val cursor = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(
                            MediaStore.Video.Media._ID,
                            MediaStore.Video.Media.DATE_ADDED,
                            MediaStore.Video.Media.DATA,
                            MediaStore.Video.Media.MIME_TYPE,
                            MediaStore.Video.Media.ALBUM,
                            MediaStore.Video.Media.DATE_TAKEN
                    ),
                    "${MediaStore.Video.Media._ID} = $fileId",
                    null,
                    null)

            val mediaMetadataRetriever = MediaMetadataRetriever()

            val mediaFile =
                    if (cursor != null && cursor.moveToFirst()) {
                        cursorToMediaFilePriorTo29(mediaMetadataRetriever, cursor, startDate = null, endDate = null)
                    } else {
                        null
                    }
            mediaMetadataRetriever.release()

            return mediaFile
        }
    }

    fun fetchVideos(
            context: Context,
            albumHashMap: MutableMap<Long, Album>,
            startDate: Long? = null,
            endDate: Long? = null
    ) {
        var selectionClause: String? = null
        var selectionArgs: Array<String>? = null

        if (startDate != null && endDate != null) {
            selectionClause = "${MediaStore.Video.Media.DATE_ADDED} BETWEEN ? AND ? OR ${MediaStore.Video.Media.DATE_TAKEN} BETWEEN ? AND ?"
            selectionArgs = arrayOf(startDate.toString(), endDate.toString(), (startDate * 1000L).toString(), (endDate * 1000L).toString())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cursor = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(
                            MediaStore.Video.Media._ID,
                            MediaStore.Video.Media.DATE_ADDED,
                            MediaStore.Video.Media.DATA,
                            MediaStore.Video.Media.MIME_TYPE,
                            MediaStore.Video.Media.BUCKET_ID,
                            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                            MediaStore.Video.Media.DURATION,
                            MediaStore.Video.Media.ORIENTATION,
                            MediaStore.Video.Media.DATE_TAKEN),
                    selectionClause,
                    selectionArgs,
                    "${MediaStore.Video.Media._ID} DESC")

            while (cursor?.moveToNext() == true) {
                val mediaFile = cursorToMediaFileSubsequent29(cursor, startDate, endDate)

                if (mediaFile != null) {
                    val album = albumHashMap[mediaFile.albumId]
                    if (album == null) {
                        albumHashMap[mediaFile.albumId] = Album(
                                mediaFile.albumId,
                                mediaFile.albumName,
                                mutableListOf(mediaFile)
                        )
                    } else {
                        album.files.add(mediaFile)
                    }
                }
            }
        } else {
            val cursor = context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(
                            MediaStore.Video.Media._ID,
                            MediaStore.Video.Media.DATE_ADDED,
                            MediaStore.Video.Media.DATA,
                            MediaStore.Video.Media.MIME_TYPE,
                            MediaStore.Video.Media.ALBUM,
                            MediaStore.Video.Media.DATE_TAKEN
                    ),
                    selectionClause,
                    selectionArgs,
                    "${MediaStore.Video.Media._ID} DESC")

            val mediaMetadataRetriever = MediaMetadataRetriever()
            while (cursor?.moveToNext() == true) {
                val mediaFile = cursorToMediaFilePriorTo29(mediaMetadataRetriever, cursor, startDate, endDate)

                if (mediaFile != null) {
                    val album = albumHashMap[mediaFile.albumId]
                    if (album == null) {
                        albumHashMap[mediaFile.albumId] = Album(
                                mediaFile.albumId,
                                mediaFile.albumName,
                                mutableListOf(mediaFile)
                        )
                    } else {
                        album.files.add(mediaFile)
                    }
                }
            }
            mediaMetadataRetriever.release()
        }
    }


    private fun cursorToMediaFileSubsequent29(cursor: Cursor, startDate: Long?, endDate: Long?): MediaFile? {
        val fileId = cursor.getLong(0)
        val fileDateAdded = cursor.getLong(1)
        val filePath = cursor.getString(2)
        val mimeType = cursor.getString(3)
        val albumId = cursor.getLong(4)
        val albumName = cursor.getString(5) ?: ""
        val duration = cursor.getDouble(6) / 1000
        val orientation = cursor.getInt(7)
        val dateTaken = cursor.getLong(8) / 1000L

        var correctDate = fileDateAdded
        var shouldReturnMediaFile = true

        // should we use takenDate or fileDateAdded? If fileDate is in range but dateTaken is not,
        // we should filter out those results. fileDateAdded is acceptable only if dateTaken is 0
        if (startDate != null && endDate != null){
            if (dateTaken in startDate..endDate){
                correctDate = dateTaken
            } else if( dateTaken != 0L){
                shouldReturnMediaFile = false
            }
        }

        if (shouldReturnMediaFile) {
            return MediaFile(
                    fileId,
                    albumId,
                    albumName,
                    correctDate,
                    filePath,
                    null,
                    orientation,
                    mimeType,
                    duration,
                    MediaFile.MediaType.VIDEO
            )
        } else {
            return null
        }
    }

    private fun cursorToMediaFilePriorTo29(
            mediaMetadataRetriever: MediaMetadataRetriever,
            cursor: Cursor,
            startDate: Long?,
            endDate: Long?
    ): MediaFile? {
        val fileId = cursor.getLong(0)
        val fileDateAdded = cursor.getLong(1)
        val filePath = cursor.getString(2)
        val mimeType = cursor.getString(3)
        val albumName = cursor.getString(4) ?: ""
        val dateTaken = cursor.getLong(5) / 1000L

        var duration: Double? = null
        var orientation: Int = -1
        try {
            mediaMetadataRetriever.setDataSource(filePath)
            duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toDouble() / 1000
            orientation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var correctDate = fileDateAdded
        var shouldReturnMediaFile = true

        // should we use takenDate or fileDateAdded? If fileDate is in range but dateTaken is not,
        // we should filter out those results. fileDateAdded is acceptable only if dateTaken is 0
        if (startDate != null && endDate != null){
            if (dateTaken in startDate..endDate){
                correctDate = dateTaken
            } else if( dateTaken != 0L){
                shouldReturnMediaFile = false
            }
        }

        if (shouldReturnMediaFile){
        return MediaFile(
                fileId,
                0,
                albumName,
                correctDate,
                filePath,
                null,
                orientation,
                mimeType,
                duration,
                MediaFile.MediaType.VIDEO
        )
        } else {
            return null
        }
    }
}