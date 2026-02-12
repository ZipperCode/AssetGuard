package com.zipper.compose.assetguard.data.backup

import com.zipper.compose.assetguard.data.local.entity.LoanStatus
import com.zipper.compose.assetguard.di.AppContainer

class DataIntegrityChecker(private val container: AppContainer) {

    data class IntegrityReport(
        val orphanRepayments: Int = 0,
        val orphanLoans: Int = 0,
        val statusMismatches: Int = 0,
        val issues: List<String> = emptyList()
    ) {
        val isHealthy: Boolean get() = issues.isEmpty()

        val summary: String
            get() = if (isHealthy) "数据完整性检查通过"
            else "发现 ${issues.size} 个问题:\n${issues.joinToString("\n")}"
    }

    suspend fun check(): IntegrityReport {
        val issues = mutableListOf<String>()
        var orphanRepayments = 0
        var orphanLoans = 0
        var statusMismatches = 0

        // 检查孤儿借条（关联的人员不存在）
        val allLoans = container.loanRepository.getAll()
        for (loan in allLoans) {
            val person = container.personDao.getById(loan.personId)
            if (person == null) {
                orphanLoans++
                issues.add("借条 #${loan.id} 关联的联系人 #${loan.personId} 不存在")
            }
        }

        // 检查孤儿还款（关联的借条不存在）
        val allRepayments = container.repaymentRepository.getAll()
        for (repayment in allRepayments) {
            val loan = container.loanRepository.getById(repayment.loanId)
            if (loan == null) {
                orphanRepayments++
                issues.add("还款 #${repayment.id} 关联的借条 #${repayment.loanId} 不存在")
            }
        }

        // 检查状态一致性（已还清状态但实际还款不足）
        for (loan in allLoans) {
            val totalRepaid = container.repaymentRepository.getTotalRepaidForLoan(loan.id)
            val expectedStatus = LoanStatus.fromRepaid(totalRepaid, loan.amount)

            // 只检查基础状态不一致（UNPAID, PARTIAL, PAID），不检查手动设置的特殊状态
            if (loan.status in listOf(LoanStatus.UNPAID, LoanStatus.PARTIAL, LoanStatus.PAID)
                && loan.status != expectedStatus
            ) {
                statusMismatches++
                issues.add("借条 #${loan.id} 状态为 ${LoanStatus.toDisplayString(loan.status)}，但实际应为 ${LoanStatus.toDisplayString(expectedStatus)}")
            }
        }

        return IntegrityReport(
            orphanRepayments = orphanRepayments,
            orphanLoans = orphanLoans,
            statusMismatches = statusMismatches,
            issues = issues
        )
    }
}
