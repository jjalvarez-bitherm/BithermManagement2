package com.bithermmanagement.chat.data.enums

enum class MessageStatus {
    SENDING,        // Mensaje enviándose (⏳)
    SENT,          // Mensaje enviado (✓)
    DELIVERED,     // Mensaje entregado al destinatario (✓✓)
    READ,          // Mensaje leído (✓✓ azul)
    FAILED,        // Mensaje falló al enviar (❌)
    PENDING        // Mensaje pendiente de envío (cuando usuario offline)
}
