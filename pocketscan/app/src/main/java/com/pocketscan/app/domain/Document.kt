package com.pocketscan.app.domain

data class Document(
    val id: Long,
    val name: String,
    val pdfPath: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val thumbnailPath: String?,
)
