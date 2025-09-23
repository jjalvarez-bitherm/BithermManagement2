package com.bithermmanagement.chat.data.converters

import androidx.room.TypeConverter
import com.bithermmanagement.chat.data.enums.MessageType

class MessageTypeConverter {
    @TypeConverter
    fun fromMessageType(value: MessageType): String {
        return value.name
    }

    @TypeConverter
    fun toMessageType(value: String): MessageType {
        return MessageType.valueOf(value)
    }
}
