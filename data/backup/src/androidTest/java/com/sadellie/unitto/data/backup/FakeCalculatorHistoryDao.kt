/*
 * Unitto is a unit converter for Android
 * Copyright (c) 2023 Elshan Agaev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sadellie.unitto.data.backup

import com.sadellie.unitto.data.database.CalculatorHistoryDao
import com.sadellie.unitto.data.database.CalculatorHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

var calculatorHistory: List<CalculatorHistoryEntity> = listOf(
    CalculatorHistoryEntity(
        timestamp = System.currentTimeMillis(),
        expression = "2+2",
        result = "4"
    )
)

object FakeCalculatorHistoryDao: CalculatorHistoryDao {
    override fun getAllDescending(): Flow<List<CalculatorHistoryEntity>> = flow {
        emit(calculatorHistory)
    }

    override suspend fun insert(vararg historyEntity: CalculatorHistoryEntity) {
        calculatorHistory += historyEntity
    }

    override suspend fun clear() {
        calculatorHistory = emptyList()
    }
}