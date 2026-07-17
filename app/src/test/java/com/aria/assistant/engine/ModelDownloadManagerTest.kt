package com.aria.assistant.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ModelDownloadManagerTest {

    @Test
    fun concurrentFileDownloadsUseOneAtomicWriter() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val output = context.cacheDir.resolve("download-${System.nanoTime()}.bin")
        val payload = ByteArray(256 * 1024) { index -> (index % 251).toByte() }
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(payload)))
            start()
        }

        try {
            val manager = ModelDownloadManager(context)
            val url = server.url("/model").toString()
            coroutineScope {
                listOf(
                    async { manager.downloadFile(url, output) },
                    async { manager.downloadFile(url, output) },
                ).awaitAll()
            }

            assertEquals(1, server.requestCount)
            assertArrayEquals(payload, output.readBytes())
            assertFalse(output.resolveSibling("${output.name}.part").exists())
        } finally {
            server.shutdown()
            output.delete()
            output.resolveSibling("${output.name}.part").delete()
        }
    }
}
