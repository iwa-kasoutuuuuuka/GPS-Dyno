package com.example.gpsdyno.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gpsdyno.domain.model.LogPoint
import com.example.gpsdyno.domain.model.LogSession
import com.example.gpsdyno.domain.model.VehicleSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [LogSession::class, LogPoint::class, VehicleSettings::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gps_dyno.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // 初回起動時にデフォルトの車両設定をデータベースへ挿入する
                        CoroutineScope(Dispatchers.IO).launch {
                            getDatabase(context).logDao().insertOrUpdateSettings(VehicleSettings())
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
