package com.microsoft.notes.platform.files

import android.util.AtomicFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class AtomicFileOutStream(file: File) : OutputStream() {
    private val atomicFile = AtomicFile(file)
    private val out = atomicFile.startWrite()

    private var _writeSuccessfullyCompleted = false
    var writeSuccessfullyCompleted: Boolean
        get() = synchronized(lock = this, block = {
            return _writeSuccessfullyCompleted
        })
        set(value) = synchronized(lock = this, block = {
            _writeSuccessfullyCompleted = value
        })

    private var _isClosed = false
    private var isClosed: Boolean
        get() = synchronized(lock = this, block = {
            return _isClosed
        })
        set(value) = synchronized(lock = this, block = {
            _isClosed = value
        })

    override fun write(b: Int) {
        this.out.write(b)
    }

    override fun write(b: ByteArray?) {
        this.out.write(b)
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
        this.out.write(b, off, len)
    }

    override fun flush() {
        this.out.flush()
    }

    override fun close() {
        if (!isClosed) {
            if (writeSuccessfullyCompleted) {
                this.atomicFile.finishWrite(this.out)
            } else {
                this.atomicFile.failWrite(this.out)
            }
        }
    }
}

class AtomicInputStream(file: File) : InputStream() {
    private val atomicFile = AtomicFile(file)
    private val inputStream = atomicFile.openRead()

    override fun skip(n: Long): Long = this.inputStream.skip(n)

    override fun available(): Int = this.inputStream.available()

    override fun reset() {
        this.inputStream.reset()
    }

    override fun close() {
        this.inputStream.close()
    }

    override fun mark(readlimit: Int) {
        this.inputStream.mark(readlimit)
    }

    override fun markSupported(): Boolean = this.inputStream.markSupported()

    override fun read(): Int = this.inputStream.read()

    override fun read(b: ByteArray?): Int = this.inputStream.read(b)

    override fun read(b: ByteArray?, off: Int, len: Int): Int = this.inputStream.read(b, off, len)
}
