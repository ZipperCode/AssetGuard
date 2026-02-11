package com.zipper.compose.assetguard.data.backup

import android.content.Context
import android.net.Uri
import com.zipper.compose.assetguard.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DatabaseBackupManager(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun exportDatabase(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 确保所有 WAL 数据写入主数据库文件
            database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            if (!dbFile.exists()) {
                return@withContext Result.failure(IllegalStateException("数据库文件不存在"))
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                dbFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importDatabase(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.close()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            // 删除 WAL 和 SHM 文件
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                dbFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
