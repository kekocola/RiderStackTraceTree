package com.example.rider.stacktrace.tree.parser

import com.example.rider.stacktrace.tree.model.FrameEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CSharpStackTraceParserTest {
    private val parser = CSharpStackTraceParser()

    @Test
    fun parsesStandardCSharpFrameWithWindowsDriveColon() {
        val entries = parser.parse(
            """   at ET.Server.ActorHandleHelper.HandleIActorRequest(Int64 actorId, IActorRequest iActorRequest) in D:\Server2\pokeworld_dev\Server\GameServer\DotNet\Hotfix\Code\Module\Actor\ActorHandleHelper.cs:line 65"""
        )

        val frame = assertIs<FrameEntry>(entries.single()).frame
        assertEquals("ET.Server.ActorHandleHelper.HandleIActorRequest(Int64 actorId, IActorRequest iActorRequest)", frame.method)
        assertEquals("""D:\Server2\pokeworld_dev\Server\GameServer\DotNet\Hotfix\Code\Module\Actor\ActorHandleHelper.cs""", frame.absolutePathFromLog)
        assertEquals("ActorHandleHelper.cs", frame.fileName)
        assertEquals(65, frame.lineNumber)
    }
}
