package com.compose.widget.picker

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalTime

@Composable
fun TimePicker(
    modifier: Modifier = Modifier,
    is24TimeFormat: Boolean,
    itemHeight: Dp = 32.dp,
    divider: NumberPickerDivider = NumberPickerDivider(),
    itemStyles: ItemStyles = ItemStyles(),
    currentTime: LocalTime,
    onTimeChanged: (LocalTime) -> Unit
) {

    val pickerTime by remember {
        mutableStateOf(
            parseTime(
                currentTime,
                is24TimeFormat = is24TimeFormat
            )
        )
    }

    val hours = (if (is24TimeFormat) (0..23) else (1..12)).toList()
    val minutes = (0..59).toList()

    Row(horizontalArrangement = Arrangement.Center, modifier = modifier.fillMaxWidth(1f)) {
        WheelPicker(
            itemHeight = itemHeight,
            divider = divider,
            itemStyles = itemStyles,
            items = hours,
            selectedItem = pickerTime.hours,
            itemToString = { String.format("%02d", it) },
            modifier = Modifier
                .fillMaxHeight(1f)
                .fillMaxWidth(.3f),
            onItemChanged = {
                pickerTime.hours = it
                onTimeChanged(pickerTime.toLocalTime())
            }
        )
        WheelPicker(
            items = minutes,
            selectedItem = pickerTime.minutes,
            itemHeight = itemHeight,
            divider = divider,
            itemStyles = itemStyles,
            itemToString = { String.format("%02d", it) },
            modifier = Modifier
                .fillMaxHeight(1f)
                .fillMaxWidth(.3f),
            onItemChanged = {
                pickerTime.minutes = it
                onTimeChanged(pickerTime.toLocalTime())
            }

        )
        if (is24TimeFormat.not()) {
            AmPmPicker(
                itemHeight = itemHeight,
                divider = divider,
                itemStyles = itemStyles,
                modifier = Modifier
                    .fillMaxHeight(1f)
                    .fillMaxWidth(.3f),
                selectedItem = pickerTime.timesOfDay!!,
                onItemChanged = {
                    pickerTime.timesOfDay = it
                    onTimeChanged(pickerTime.toLocalTime())
                }
            )
        }
    }
}

@Composable
fun AmPmPicker(
    modifier: Modifier = Modifier,
    selectedItem: TimesOfDay,
    itemHeight: Dp = 32.dp,
    divider: NumberPickerDivider = NumberPickerDivider(showed = true, Color.Black),
    itemStyles: ItemStyles = ItemStyles(),
    onItemChanged: (TimesOfDay) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var currItem by remember { mutableStateOf(selectedItem) }
    var listHeightInPixels by remember { mutableStateOf(0) }
    var itemHeightInPixels by remember { mutableStateOf(0) }

    val items = TimesOfDay.values()

    val listState =
        rememberLazyListState(initialFirstVisibleItemIndex = items.indexOf(selectedItem))

    if (listState.isScrollInProgress.not() && itemHeightInPixels > 0 && listHeightInPixels > itemHeightInPixels) {
        if (listState.firstVisibleItemScrollOffset != 0) {
            LaunchedEffect(key1 = listState) {
                scope.launch {
                    listState.animateScrollBy(
                        (if (listState.firstVisibleItemScrollOffset <= itemHeightInPixels / 2)
                            -listState.firstVisibleItemScrollOffset - itemHeightInPixels
                        else listState.firstVisibleItemScrollOffset + itemHeightInPixels).toFloat()
                    )
                }
            }
        } else {
            if (items[listState.firstVisibleItemIndex] != currItem) {
                currItem = items[listState.firstVisibleItemIndex]
                onItemChanged(currItem)
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (items.isNotEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(top = itemHeight, bottom = itemHeight),
                state = listState,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(itemHeight * (items.size + 1))
                    .onGloballyPositioned {
                        if (listHeightInPixels < itemHeightInPixels) {
                            listHeightInPixels = it.size.height

                            scope.launch {
                                listState.scrollToItem(items.indexOf(selectedItem))
                            }
                        }
                    }
            ) {
                items(items.size) { i ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .height(itemHeight)
                            .onGloballyPositioned {
                                if (itemHeightInPixels == 0) {
                                    itemHeightInPixels = it.size.height
                                }
                            }
                    ) {
                        Text(
                            text = items[i].string,
                            style = if (i == listState.firstVisibleItemIndex) itemStyles.selectedTextStyle else itemStyles.defaultTextStyle
                        )
                    }
                }
            }
        }
        if (divider.showed) {
            Divider(
                color = divider.color,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .offset(y = -itemHeight / 2)
                    .offset(x = -divider.indent),
                thickness = divider.thickness,
                startIndent = divider.indent * 2
            )
            Divider(
                color = divider.color, modifier = Modifier
                    .fillMaxWidth(1f)
                    .offset(y = itemHeight / 2)
                    .offset(x = -divider.indent),
                thickness = divider.thickness,
                startIndent = divider.indent * 2
            )
        }
    }
}

enum class TimesOfDay(val string: String) {
    AM("AM"), PM("PM")
}


fun parseTime(time: LocalTime, is24TimeFormat: Boolean): PickerTime {
    return PickerTime(
        hours = if (is24TimeFormat.not() && time.hour > 12) time.hour - 12 else time.hour,
        minutes = time.minute,
        if (is24TimeFormat.not() && time.hour > 12) TimesOfDay.PM else TimesOfDay.AM
    )
}

fun PickerTime.toLocalTime(): LocalTime {
    return LocalTime.of(
        when (timesOfDay) {
            TimesOfDay.AM -> hours
            TimesOfDay.PM -> hours + 12
            else -> hours
        },
        minutes
    )
}

class PickerTime(
    var hours: Int,
    var minutes: Int,
    var timesOfDay: TimesOfDay? = null
) {
    override fun toString(): String {
        return "${String.format("%02d", hours)}:${
            String.format(
                "%02d",
                minutes
            )
        } ${timesOfDay?.string ?: ""}"
    }
}
