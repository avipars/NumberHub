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

package com.sadellie.unitto.feature.converter

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadellie.unitto.core.base.OutputFormat
import com.sadellie.unitto.core.base.R
import com.sadellie.unitto.core.ui.common.UnittoSearchBar
import com.sadellie.unitto.core.ui.common.textfield.FormatterSymbols
import com.sadellie.unitto.core.ui.common.textfield.formatExpression
import com.sadellie.unitto.data.common.format
import com.sadellie.unitto.data.model.UnitGroup
import com.sadellie.unitto.data.model.UnitsListSorting
import com.sadellie.unitto.data.model.unit.AbstractUnit
import com.sadellie.unitto.data.model.unit.DefaultUnit
import com.sadellie.unitto.data.model.unit.NormalUnit
import com.sadellie.unitto.data.model.unit.NumberBaseUnit
import com.sadellie.unitto.data.units.MyUnitIDS
import com.sadellie.unitto.feature.converter.components.BasicUnitListItem
import com.sadellie.unitto.feature.converter.components.FavoritesButton
import com.sadellie.unitto.feature.converter.components.SearchPlaceholder
import com.sadellie.unitto.feature.converter.components.UnitGroupHeader
import java.math.BigDecimal

@Composable
internal fun RightSideRoute(
    viewModel: ConverterViewModel,
    navigateUp: () -> Unit,
    navigateToUnitGroups: () -> Unit,
) {
    when (
        val uiState = viewModel.rightSideUIState.collectAsStateWithLifecycle().value
    ) {
        is RightSideUIState.Loading -> Box(Modifier.fillMaxSize())
        is RightSideUIState.Ready ->
            RightSideScreen(
                uiState = uiState,
                onQueryChange = viewModel::queryChangeRight,
                toggleFavoritesOnly = viewModel::favoritesOnlyChange,
                updateUnitTo = viewModel::updateUnitTo,
                favoriteUnit = viewModel::favoriteUnit,
                navigateUp = navigateUp,
                navigateToUnitGroups = navigateToUnitGroups,
            )
    }
}

@Composable
private fun RightSideScreen(
    uiState: RightSideUIState.Ready,
    onQueryChange: (TextFieldValue) -> Unit,
    toggleFavoritesOnly: (Boolean) -> Unit,
    updateUnitTo: (AbstractUnit) -> Unit,
    favoriteUnit: (AbstractUnit) -> Unit,
    navigateUp: () -> Unit,
    navigateToUnitGroups: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            UnittoSearchBar(
                query = uiState.query,
                onQueryChange = onQueryChange,
                navigateUp = navigateUp,
                title = stringResource(R.string.units_screen_to),
                placeholder = stringResource(R.string.search_bar_placeholder),
                noSearchActions = {
                    FavoritesButton(uiState.favorites) {
                        toggleFavoritesOnly(!uiState.favorites)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Crossfade(
            targetState = uiState.units.isNotEmpty(),
            modifier = Modifier.padding(paddingValues),
            label = "Units list"
        ) { hasUnits ->
            when (hasUnits) {
                true -> LazyColumn(Modifier.fillMaxSize()) {
                    uiState.units.forEach { (unitGroup, units) ->
                        item(unitGroup.name) {
                            UnitGroupHeader(Modifier.animateItemPlacement(), unitGroup)
                        }

                        items(units, { it.id }) {
                            BasicUnitListItem(
                                modifier = Modifier.animateItemPlacement(),
                                name = stringResource(it.displayName),
                                supportLabel = formatUnitToSupportLabel(
                                    unitFrom = uiState.unitFrom,
                                    unitTo = it,
                                    input = uiState.input,
                                    shortName = stringResource(it.shortName),
                                    scale = uiState.scale,
                                    outputFormat = uiState.outputFormat,
                                    formatterSymbols = uiState.formatterSymbols,
                                    readyCurrencies = uiState.currencyRateUpdateState is CurrencyRateUpdateState.Ready,
                                ),
                                isFavorite = it.isFavorite,
                                isSelected = it.id == uiState.unitTo.id,
                                onClick = {
                                    onQueryChange(TextFieldValue())
                                    updateUnitTo(it)
                                    navigateUp()
                                },
                                favoriteUnit = { favoriteUnit(it) }
                            )
                        }
                    }
                }

                false -> SearchPlaceholder(navigateToSettingsAction = navigateToUnitGroups)
            }
        }
    }
}

private fun formatUnitToSupportLabel(
    unitFrom: AbstractUnit?,
    unitTo: AbstractUnit?,
    input: String,
    shortName: String,
    scale: Int,
    outputFormat: Int,
    formatterSymbols: FormatterSymbols,
    readyCurrencies: Boolean,
): String {
    if ((unitFrom?.group == UnitGroup.CURRENCY) and !readyCurrencies) return shortName
    if (input.isEmpty()) return shortName

    try {
        if ((unitFrom is DefaultUnit) and (unitTo is DefaultUnit)) {
            unitFrom as DefaultUnit
            unitTo as DefaultUnit

            val conversion = unitFrom
                .convert(unitTo, BigDecimal(input))
                .format(scale, outputFormat)
                .formatExpression(formatterSymbols)

            return "$conversion $shortName"
        }

        if ((unitFrom is NumberBaseUnit) and (unitTo is NumberBaseUnit)) {
            unitFrom as NumberBaseUnit
            unitTo as NumberBaseUnit

            val conversion = unitFrom.convert(unitTo, input).uppercase()

            return "$conversion $shortName"
        }
    } catch (e: Exception) {
        return shortName
    }

    return shortName
}

@Preview
@Composable
private fun RightSideScreenPreview() {
    val units: Map<UnitGroup, List<AbstractUnit>> = mapOf(
        UnitGroup.LENGTH to listOf(
            NormalUnit(MyUnitIDS.meter, BigDecimal.valueOf(1.0E+18), UnitGroup.LENGTH, R.string.meter, R.string.meter_short),
            NormalUnit(MyUnitIDS.kilometer, BigDecimal.valueOf(1.0E+21), UnitGroup.LENGTH, R.string.kilometer, R.string.kilometer_short),
            NormalUnit(MyUnitIDS.nautical_mile, BigDecimal.valueOf(1.852E+21), UnitGroup.LENGTH, R.string.nautical_mile, R.string.nautical_mile_short),
            NormalUnit(MyUnitIDS.inch, BigDecimal.valueOf(25_400_000_000_000_000), UnitGroup.LENGTH, R.string.inch, R.string.inch_short),
            NormalUnit(MyUnitIDS.foot, BigDecimal.valueOf(304_800_000_000_002_200), UnitGroup.LENGTH, R.string.foot, R.string.foot_short),
            NormalUnit(MyUnitIDS.yard, BigDecimal.valueOf(914_400_000_000_006_400), UnitGroup.LENGTH, R.string.yard, R.string.yard_short),
            NormalUnit(MyUnitIDS.mile, BigDecimal.valueOf(1_609_344_000_000_010_500_000.0), UnitGroup.LENGTH, R.string.mile, R.string.mile_short),
        )
    )

    RightSideScreen(
        uiState = RightSideUIState.Ready(
            unitFrom = units.values.first().first(),
            units = units,
            query = TextFieldValue(),
            favorites = false,
            sorting = UnitsListSorting.USAGE,
            unitTo = units.values.first()[1],
            input = "100",
            scale = 3,
            outputFormat = OutputFormat.PLAIN,
            formatterSymbols = FormatterSymbols.Spaces,
            currencyRateUpdateState = CurrencyRateUpdateState.Nothing
        ),
        onQueryChange = {},
        toggleFavoritesOnly = {},
        updateUnitTo = {},
        favoriteUnit = {},
        navigateUp = {},
        navigateToUnitGroups = {}
    )
}

