package com.example.myapplication.data.repository

import com.example.myapplication.data.security.SecureStorage

interface StockMutationRepository {
    fun getClientMutationUuid(): String
    suspend fun createGrn(payloadJson: String): Result<String>
    suspend fun materialIssue(payloadJson: String): Result<String>
    suspend fun materialReturn(payloadJson: String): Result<String>
    suspend fun materialTransfer(payloadJson: String): Result<String>
    suspend fun materialAdjustment(payloadJson: String): Result<String>
}

class StockMutationRepositoryImpl(
    private val secureStorage: SecureStorage
) : StockMutationRepository {

    override fun getClientMutationUuid(): String {
        return secureStorage.getOrCreateClientMutationUuid()
    }

    override suspend fun createGrn(payloadJson: String): Result<String> {
        return Result.failure(
            UnsupportedOperationException("Blocked: RPC signature 'create_grn' disabled until exact live RPC signature is verified with backend.")
        )
    }

    override suspend fun materialIssue(payloadJson: String): Result<String> {
        return Result.failure(
            UnsupportedOperationException("Blocked: RPC signature 'material_issue' disabled until exact live RPC signature is verified with backend.")
        )
    }

    override suspend fun materialReturn(payloadJson: String): Result<String> {
        return Result.failure(
            UnsupportedOperationException("Blocked: RPC signature 'material_return' disabled until exact live RPC signature is verified with backend.")
        )
    }

    override suspend fun materialTransfer(payloadJson: String): Result<String> {
        return Result.failure(
            UnsupportedOperationException("Blocked: RPC signature 'material_transfer' disabled until exact live RPC signature is verified with backend.")
        )
    }

    override suspend fun materialAdjustment(payloadJson: String): Result<String> {
        return Result.failure(
            UnsupportedOperationException("Blocked: RPC signature 'material_adjustment' disabled until exact live RPC signature is verified with backend.")
        )
    }
}
