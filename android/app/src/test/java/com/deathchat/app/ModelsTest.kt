package com.deathchat.app

import com.deathchat.app.data.api.models.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ModelsTest {

    private val gson = Gson()

    @Test
    fun testRoomDtoSerialization() {
        val json = """
            {
                "roomId": "DC7X9K",
                "name": "The Underground",
                "isPrivate": false,
                "messageRetentionSeconds": 3600,
                "participantCount": 5
            }
        """.trimIndent()

        val room = gson.fromJson(json, RoomDto::class.java)

        assertEquals("DC7X9K", room.roomId)
        assertEquals("The Underground", room.name)
        assertFalse(room.isPrivate)
        assertEquals(3600, room.messageRetentionSeconds)
        assertEquals(5, room.participantCount)
    }

    @Test
    fun testMessageDtoSerialization() {
        val json = """
            {
                "messageId": "msg-12345",
                "roomId": "DC7X9K",
                "senderSessionId": "sess-alpha",
                "senderDisplayName": "PhantomReaper",
                "senderAvatar": "avatar_reaper",
                "content": "Encrypted transmission test",
                "createdAt": "2026-10-13T10:00:00.000Z",
                "expiresAt": "2026-10-13T11:00:00.000Z"
            }
        """.trimIndent()

        val msg = gson.fromJson(json, MessageDto::class.java)

        assertEquals("msg-12345", msg.messageId)
        assertEquals("DC7X9K", msg.roomId)
        assertEquals("PhantomReaper", msg.senderDisplayName)
        assertEquals("avatar_reaper", msg.senderAvatar)
        assertEquals("Encrypted transmission test", msg.content)
    }

    @Test
    fun testRoomCodeExtraction() {
        val url = "http://10.0.2.2:3000/?room=DC99AA&token=xyz"
        val code = url.substringAfter("?room=").substringBefore("&")
        assertEquals("DC99AA", code)
    }
}
