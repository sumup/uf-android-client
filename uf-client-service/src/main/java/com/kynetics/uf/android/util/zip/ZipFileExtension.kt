package com.kynetics.uf.android.util.zip

import android.util.Log
import com.kynetics.uf.android.update.system.ABOtaInstaller
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

fun ZipFile.getEntryOffset(name:String): Long {
    val zipEntries = entries()
    var offset: Long = 0
    while (zipEntries.hasMoreElements()) {
        val entry = zipEntries.nextElement()
        offset += entry.getHeaderSize()
        if (entry.name == name) {
            return offset
        }
        offset += entry.compressedSize
    }
    Log.e(ABOtaInstaller.TAG, "Entry $name not found")
    throw IllegalArgumentException("The given entry was not found")
}

fun ZipEntry.getHeaderSize(): Long {
    // Each entry has an header of (30 + n + m) bytes
    // 'n' is the length of the file name
    // 'm' is the length of the extra field
    val fixedHeaderSize = 30L
    val n = name.length
    val m = extra?.size ?: 0
    return fixedHeaderSize + n + m
}