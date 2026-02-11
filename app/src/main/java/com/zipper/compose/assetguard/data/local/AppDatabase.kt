package com.zipper.compose.assetguard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zipper.compose.assetguard.data.local.dao.LoanDao
import com.zipper.compose.assetguard.data.local.dao.PaymentMethodDao
import com.zipper.compose.assetguard.data.local.dao.PersonDao
import com.zipper.compose.assetguard.data.local.dao.RepaymentDao
import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.PaymentMethodEntity
import com.zipper.compose.assetguard.data.local.entity.PersonEntity
import com.zipper.compose.assetguard.data.local.entity.RepaymentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PersonEntity::class,
        LoanEntity::class,
        RepaymentEntity::class,
        PaymentMethodEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao
    abstract fun loanDao(): LoanDao
    abstract fun repaymentDao(): RepaymentDao
    abstract fun paymentMethodDao(): PaymentMethodDao

    companion object {
        const val DATABASE_NAME = "assetguard.db"

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(PrepopulateCallback())
                .build()
        }
    }

    private class PrepopulateCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            db.execSQL("INSERT INTO payment_methods (name, isBuiltin, createdAt) VALUES ('微信', 1, ${System.currentTimeMillis()})")
            db.execSQL("INSERT INTO payment_methods (name, isBuiltin, createdAt) VALUES ('支付宝', 1, ${System.currentTimeMillis()})")
            db.execSQL("INSERT INTO payment_methods (name, isBuiltin, createdAt) VALUES ('银行卡', 1, ${System.currentTimeMillis()})")
            db.execSQL("INSERT INTO payment_methods (name, isBuiltin, createdAt) VALUES ('现金', 1, ${System.currentTimeMillis()})")
        }
    }
}
