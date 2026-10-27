package com.artiuillab.tieryourlife.feature.community.presentation.explore

import android.text.format.DateFormat
import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import java.text.NumberFormat
import java.util.Date

/** A count in a sentence: grouped the way the reader's language groups thousands, then put in its plural. */
@Composable
@ReadOnlyComposable
internal fun countText(@PluralsRes plural: Int, count: Int): String =
    pluralStringResource(plural, count, groupedCount(count))

@Composable
@ReadOnlyComposable
internal fun groupedCount(count: Int): String =
    NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0]).format(count)

/** A clock time in the reader's own format: "Showing lists from 14:20". */
@Composable
@ReadOnlyComposable
internal fun clockTime(millis: Long): String = DateFormat.getTimeFormat(LocalContext.current).format(Date(millis))
