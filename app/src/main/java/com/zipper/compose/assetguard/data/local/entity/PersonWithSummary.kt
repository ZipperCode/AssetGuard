package com.zipper.compose.assetguard.data.local.entity

import androidx.room.Embedded

data class PersonWithSummary(
    @Embedded
    val person: PersonEntity,
    val totalLent: Long, // 总借出（分）
    val totalRepaid: Long, // 已还（分）
    val unpaidLoanCount: Int // 未结清借条数
) {
    val outstanding: Long get() = totalLent - totalRepaid
}
