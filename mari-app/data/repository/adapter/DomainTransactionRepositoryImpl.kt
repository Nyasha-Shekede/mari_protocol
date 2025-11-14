package com.Mari.mobileapp.data.repository.adapter

import com.Mari.mobileapp.domain.repository.TransactionRepository as DomainTxRepo
import com.Mari.mobileapp.domain.model.Transaction as DomainTx
import com.Mari.mobileapp.domain.model.TransactionStatus as DomainStatus
import com.Mari.mobileapp.domain.model.TransactionType as DomainType
import com.Mari.mobileapp.domain.model.TransportMethod as DomainTransport
import com.Mari.mobileapp.data.repository.TransactionRepository as DataTxRepo
import com.Mari.mobileapp.data.model.Transaction as DataTx
import com.Mari.mobileapp.data.model.TransactionStatus as DataStatus
import com.Mari.mobileapp.data.model.TransactionType as DataType
import com.Mari.mobileapp.data.model.TransportMethod as DataTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainTransactionRepositoryImpl @Inject constructor(
    private val dataRepo: DataTxRepo
) : DomainTxRepo {

    override suspend fun createTransaction(transaction: DomainTx) {
        dataRepo.createTransaction(transaction.toData())
    }

    override suspend fun getTransaction(transactionId: String): DomainTx? =
        dataRepo.getTransaction(transactionId)?.toDomain()

    override fun getTransactionsForUser(bioHash: String): Flow<List<DomainTx>> =
        dataRepo.getTransactionsForUser(bioHash).map { list -> list.map { it.toDomain() } }

    override suspend fun getPendingTransactions(): List<DomainTx> =
        dataRepo.getPendingTransactions().map { it.toDomain() }

    override suspend fun getPendingSmsTransactions(): List<DomainTx> =
        dataRepo.getPendingSmsTransactions().map { it.toDomain() }

    override suspend fun updateTransaction(transaction: DomainTx) {
        dataRepo.updateTransaction(transaction.toData())
    }

    override suspend fun deleteTransaction(transactionId: String) {
        dataRepo.deleteTransaction(transactionId)
    }

    override suspend fun getTotalReceived(bioHash: String): Double =
        dataRepo.getTotalReceived(bioHash)

    override suspend fun getTotalSent(bioHash: String): Double =
        dataRepo.getTotalSent(bioHash)

    private fun DomainTx.toData(): DataTx = DataTx(
        id = id,
        senderBioHash = senderBioHash,
        receiverBioHash = receiverBioHash,
        amount = amount,
        locationGrid = locationGrid,
        timestamp = timestamp,
        status = when (status) {
            DomainStatus.PENDING -> DataStatus.PENDING
            DomainStatus.COMPLETED -> DataStatus.COMPLETED
            DomainStatus.FAILED -> DataStatus.FAILED
        },
        type = when (type) {
            DomainType.SEND -> DataType.SEND
            DomainType.RECEIVE -> DataType.RECEIVE
        },
        coupon = coupon,
        transportMethod = when (transportMethod) {
            DomainTransport.SMS -> DataTransport.SMS
            DomainTransport.RECEIVED -> DataTransport.RECEIVED
        }
    )

    private fun DataTx.toDomain(): DomainTx = DomainTx(
        id = id,
        senderBioHash = senderBioHash,
        receiverBioHash = receiverBioHash,
        amount = amount,
        locationGrid = locationGrid,
        timestamp = timestamp,
        status = when (status) {
            DataStatus.PENDING -> DomainStatus.PENDING
            DataStatus.COMPLETED -> DomainStatus.COMPLETED
            DataStatus.FAILED -> DomainStatus.FAILED
        },
        type = when (type) {
            DataType.SEND -> DomainType.SEND
            DataType.RECEIVE -> DomainType.RECEIVE
        },
        coupon = coupon,
        transportMethod = when (transportMethod) {
            DataTransport.SMS -> DomainTransport.SMS
            DataTransport.RECEIVED -> DomainTransport.RECEIVED
        }
    )
}
