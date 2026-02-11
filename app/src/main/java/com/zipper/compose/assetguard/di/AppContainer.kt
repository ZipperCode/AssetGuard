package com.zipper.compose.assetguard.di

import android.content.Context
import com.zipper.compose.assetguard.data.local.AppDatabase
import com.zipper.compose.assetguard.data.local.dao.LoanDao
import com.zipper.compose.assetguard.data.local.dao.PaymentMethodDao
import com.zipper.compose.assetguard.data.local.dao.PersonDao
import com.zipper.compose.assetguard.data.local.dao.RepaymentDao
import com.zipper.compose.assetguard.data.repository.LoanRepository
import com.zipper.compose.assetguard.data.repository.PaymentMethodRepository
import com.zipper.compose.assetguard.data.repository.PersonRepository
import com.zipper.compose.assetguard.data.repository.RepaymentRepository

class AppContainer(context: Context) {

    private val database: AppDatabase = AppDatabase.create(context)

    val personDao: PersonDao = database.personDao()
    val loanDao: LoanDao = database.loanDao()
    val repaymentDao: RepaymentDao = database.repaymentDao()
    val paymentMethodDao: PaymentMethodDao = database.paymentMethodDao()

    val personRepository: PersonRepository by lazy { PersonRepository(personDao) }
    val loanRepository: LoanRepository by lazy { LoanRepository(loanDao) }
    val repaymentRepository: RepaymentRepository by lazy { RepaymentRepository(repaymentDao, loanDao) }
    val paymentMethodRepository: PaymentMethodRepository by lazy { PaymentMethodRepository(paymentMethodDao) }

    fun getDatabase(): AppDatabase = database
}
