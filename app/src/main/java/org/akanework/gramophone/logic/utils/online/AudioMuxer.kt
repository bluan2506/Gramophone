package org.akanework.gramophone.logic.utils.online

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * Extracts audio from a progressive mp4 video file (e.g. itag 18: H.264 + AAC) into an audio-only
 * file, using Android's built-in [MediaExtractor] + [MediaMuxer]. No re-encode — it just copies the
 * already-compressed audio samples into a new mp4/m4a container, so it runs almost instantly.
 * Android has no built-in MP3 encoder, so the audio keeps its original codec (AAC) and is saved as
 * .m4a. Ported from the MSDownloader reference.
 */
object AudioMuxer {

    private const val TAG = "AudioMuxer"
    private const val MAX_SAMPLE_SIZE = 512 * 1024

    /**
     * If [path] has a video track (a 360p file downloaded because the audio link 403'd), extract the
     * audio track into a new .m4a file and delete the original video. Files that are already
     * audio-only are left untouched. Must run on a background thread.
     *
     * @return the path to feed into the library: the new .m4a if converted, or [path] unchanged.
     */
    @JvmStatic
    fun extractAudioIfHasVideo(path: String): String {
        val src = File(path)
        if (!src.exists() || src.length() == 0L) return path

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        val tmp = File(src.parent, src.name + ".audio.tmp")
        try {
            extractor.setDataSource(path)

            var hasVideo = false
            var audioTrack = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                when {
                    mime.startsWith("video/") -> hasVideo = true
                    mime.startsWith("audio/") && audioTrack < 0 -> {
                        audioTrack = i
                        audioFormat = f
                    }
                }
            }

            // Already audio-only (normal audio link) -> no conversion needed.
            if (!hasVideo || audioTrack < 0 || audioFormat == null) return path

            extractor.selectTrack(audioTrack)
            muxer = MediaMuxer(tmp.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val buffer = ByteBuffer.allocate(MAX_SAMPLE_SIZE)
            val info = MediaCodec.BufferInfo()
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                info.offset = 0
                info.size = size
                info.presentationTimeUs = extractor.sampleTime
                // Audio samples are all sync frames -> flag KEY_FRAME for the muxer.
                info.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                muxer.writeSampleData(outTrack, buffer, info)
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null

            // Write the new .m4a first, delete the original video after -> if rename fails the
            // original file is still intact, no data loss.
            val dst = uniqueM4a(src)
            if (tmp.renameTo(dst)) {
                src.delete()
                return dst.absolutePath
            }
            return path
        } catch (e: Exception) {
            Log.e(TAG, "extractAudioIfHasVideo failed: $e")
            return path
        } finally {
            try {
                muxer?.release()
            } catch (_: Exception) {
            }
            extractor.release()
            if (tmp.exists()) tmp.delete()
        }
    }

    /** A non-existing .m4a filename derived from the source name (old extension dropped). */
    private fun uniqueM4a(src: File): File {
        val base = src.name.substringBeforeLast('.')
        var candidate = File(src.parent, "$base.m4a")
        var i = 0
        while (candidate.exists()) {
            i++
            candidate = File(src.parent, "$base-$i.m4a")
        }
        return candidate
    }
}
