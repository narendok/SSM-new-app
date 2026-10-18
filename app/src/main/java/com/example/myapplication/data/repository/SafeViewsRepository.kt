package com.example.myapplication.data.repository

import com.example.myapplication.data.model.GenericViewRecord
import com.example.myapplication.data.model.SafeViewName

interface SafeViewsRepository {
    suspend fun fetchSafeViewRecords(
        view: SafeViewName
    ): Result<List<GenericViewRecord>>
}

class SafeViewsRepositoryImpl : SafeViewsRepository {
    override suspend fun fetchSafeViewRecords(
        view: SafeViewName
    ): Result<List<GenericViewRecord>> {
        // Explicitly unimplemented without fake network calls or fabricated columns
        // until live DB schema is verified with backend.
        return Result.failure(
            NotImplementedError("Blocked: Awaiting verified live schema from backend team for safe view '${view.viewIdentifier}'. No fake queries executed.")
        )
    }
}
