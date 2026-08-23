package com.zedge.automation.ui.screens

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min

data class AudioProcessResult(val data: ByteArray, val mimeType: String)

fun processAudio(mp3Bytes: ByteArray, gain: Float, silenceThreshold: Float, padMs: Int): AudioProcessResult {
    val tempIn = File.createTempFile("process_in", ".mp3")
    tempIn.writeBytes(mp3Bytes)
    try {
        val pcm = decodeToPcm(tempIn)
        if (pcm.isEmpty()) return AudioProcessResult(mp3Bytes, "audio/mpeg")

        val sampleRate = 44100
        val channels = 2
        val bytesPerSample = 2 * channels

        val gained = applyGain(pcm, gain)
        val trimmed = trimSilence(gained, silenceThreshold, sampleRate, bytesPerSample)

        val padBytes = (padMs * sampleRate * bytesPerSample / 1000).toInt()
        val padded = ByteArray(padBytes + trimmed.size + padBytes)
        System.arraycopy(trimmed, 0, padded, padBytes, trimmed.size)

        val wavBytes = encodeToWav(padded, sampleRate, channels)
        return AudioProcessResult(wavBytes, "audio/wav")
    } catch (e: Exception) {
        return AudioProcessResult(mp3Bytes, "audio/mpeg")
    } finally {
        tempIn.delete()
    }
}

private fun encodeToWav(pcmData: ByteArray, sampleRate: Int, channels: Int): ByteArray {
    val bitsPerSample = 16
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8
    val dataSize = pcmData.size
    val totalSize = 36 + dataSize

    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray())
    header.putInt(totalSize)
    header.put("WAVE".toByteArray())
    header.put("fmt ".toByteArray())
    header.putInt(16)
    header.putShort(1)
    header.putShort(channels.toShort())
    header.putInt(sampleRate)
    header.putInt(byteRate)
    header.putShort(blockAlign.toShort())
    header.putShort(bitsPerSample.toShort())
    header.put("data".toByteArray())
    header.putInt(dataSize)

    val output = ByteArrayOutputStream(44 + dataSize)
    output.write(header.array())
    output.write(pcmData)
    return output.toByteArray()
}

private fun decodeToPcm(file: File): ByteArray {
    val extractor = MediaExtractor()
    extractor.setDataSource(file.absolutePath)

    var format: MediaFormat? = null
    var trackIndex = 0
    for (i in 0 until extractor.trackCount) {
        val f = extractor.getTrackFormat(i)
        if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
            format = f
            trackIndex = i
            break
        }
    }
    if (format == null) { extractor.release(); return ByteArray(0) }
    extractor.selectTrack(trackIndex)

    val mime = format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
    val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
    val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

    val pcmFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW, sampleRate, channelCount)
    pcmFormat.setInteger(MediaFormat.KEY_ENCODING, MediaFormat.ENCODING_PCM_16BIT)

    val codec = MediaCodec.createDecoderByType(mime)
    codec.configure(format, null, null, 0)
    codec.start()

    val info = MediaCodec.BufferInfo()
    val output = ByteArrayOutputStream()
    var inputDone = false
    var outputDone = false

    while (!outputDone) {
        if (!inputDone) {
            val inIdx = codec.dequeueInputBuffer(10_000)
            if (inIdx >= 0) {
                val inBuf = codec.getInputBuffer(inIdx) ?: continue
                val read = extractor.readSampleData(inBuf, 0)
                if (read < 0) {
                    codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    inputDone = true
                } else {
                    codec.queueInputBuffer(inIdx, 0, read, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }
        }

        val outIdx = codec.dequeueOutputBuffer(info, 10_000)
        if (outIdx >= 0) {
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
            if (info.size > 0) {
                val outBuf = codec.getOutputBuffer(outIdx) ?: continue
                val data = ByteArray(info.size)
                outBuf.get(data)
                output.write(data)
            }
            codec.releaseOutputBuffer(outIdx, false)
        }
    }

    codec.stop()
    codec.release()
    extractor.release()
    return output.toByteArray()
}

private fun applyGain(pcm: ByteArray, gain: Float): ByteArray {
    val buf = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
    val out = ByteArray(pcm.size)
    val outBuf = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
    while (buf.hasRemaining()) {
        val sample = buf.getShort().toFloat() * gain
        outBuf.putShort(sample.toInt().coerceIn(-32768, 32767).toShort())
    }
    return out
}

private fun trimSilence(pcm: ByteArray, threshold: Float, sampleRate: Int, bytesPerSample: Int): ByteArray {
    val buf = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
    val samples = mutableListOf<Short>()
    while (buf.hasRemaining()) {
        samples.add(buf.getShort())
    }
    if (samples.isEmpty()) return pcm

    val frameSize = bytesPerSample / 2
    val thresholdShort = (threshold * 32767).toInt().toShort()

    var startIdx = 0
    for (i in 0 until samples.size - frameSize step frameSize) {
        var maxAmp = 0
        for (j in 0 until frameSize) {
            maxAmp = maxOf(maxAmp, abs(samples[i + j].toInt()))
        }
        if (maxAmp > thresholdShort) { startIdx = i; break }
    }

    var endIdx = samples.size
    for (i in samples.size - frameSize downTo 0 step frameSize) {
        var maxAmp = 0
        for (j in 0 until frameSize) {
            if (i + j < samples.size) maxAmp = maxOf(maxAmp, abs(samples[i + j].toInt()))
        }
        if (maxAmp > thresholdShort) { endIdx = min(i + frameSize, samples.size); break }
    }

    val trimmed = samples.subList(startIdx, endIdx)
    val out = ByteArray(trimmed.size * 2)
    val outBuf = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
    for (s in trimmed) outBuf.putShort(s)
    return out
}
