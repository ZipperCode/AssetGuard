package com.zipper.compose.assetguard.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PaymentMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("personId"),
        Index("paymentMethodId")
    ]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val personId: Long,
    val amount: Long, // 以分为单位
    val loanDate: Long, // 实际借款日期
    val dueDate: Long? = null, // 到期日，可选
    val paymentMethodId: Long,
    val note: String? = null,
    val status: Int = LoanStatus.UNPAID, // 0=未还 1=部分 2=已还
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object LoanStatus {
    const val UNPAID = 0
    const val PARTIAL = 1
    const val PAID = 2

    fun fromRepaid(repaidAmount: Long, totalAmount: Long): Int = when {
        repaidAmount >= totalAmount -> PAID
        repaidAmount > 0 -> PARTIAL
        else -> UNPAID
    }

    fun toDisplayString(status: Int): String = when (status) {
        UNPAID -> "未还"
        PARTIAL -> "部分归还"
        PAID -> "已还清"
        else -> "未知"
    }
}
