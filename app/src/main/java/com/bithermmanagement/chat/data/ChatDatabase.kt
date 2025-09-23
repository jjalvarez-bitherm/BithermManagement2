package com.bithermmanagement.chat.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.bithermmanagement.chat.data.dao.ChatMessageDao
import com.bithermmanagement.chat.data.dao.ChatGroupDao
import com.bithermmanagement.chat.data.dao.ChatUserDao
import com.bithermmanagement.chat.data.dao.UserConnectionDao
import com.bithermmanagement.chat.data.entities.ChatMessageEntity
import com.bithermmanagement.chat.data.entities.ChatGroupEntity
import com.bithermmanagement.chat.data.entities.ChatUserEntity
import com.bithermmanagement.chat.data.entities.UserConnectionEntity
import com.bithermmanagement.chat.data.converters.DateConverter
import com.bithermmanagement.chat.data.converters.MessageTypeConverter

@Database(
    entities = [
        ChatMessageEntity::class,
        ChatGroupEntity::class,
        ChatUserEntity::class,
        UserConnectionEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverter::class, MessageTypeConverter::class)
abstract class ChatDatabase : RoomDatabase() {
    
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatGroupDao(): ChatGroupDao
    abstract fun chatUserDao(): ChatUserDao
    abstract fun userConnectionDao(): UserConnectionDao
    
    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null
        
        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
