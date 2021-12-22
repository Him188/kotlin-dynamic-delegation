package compiler

import java.io.OutputStream

class CombinedOutputStream(
    private vararg val streams: OutputStream
) : OutputStream() {
    override fun write(b: Int) {
        streams.forEach { it.write(b) }
    }

    override fun write(b: ByteArray) {
        streams.forEach { it.write(b) }
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        streams.forEach { it.write(b, off, len) }
    }

    override fun close() {
        streams.forEach { it.close() }
    }

    override fun flush() {
        streams.forEach { it.flush() }
    }
}