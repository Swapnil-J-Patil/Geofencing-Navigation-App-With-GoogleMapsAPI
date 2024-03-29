package com.swapnil.googlenavigationapp.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swapnil.googlenavigationapp.R
import com.swapnil.googlenavigationapp.data.RouteStep


@Composable
fun StepCard(currentStep: RouteStep, nextStep: RouteStep) {
    Card(
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            colorResource(id = R.color.darkBlue),
                            colorResource(id = R.color.lightBlue)
                        )
                    )
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                val currentDistance = currentStep.distance
                val nextManeuver = nextStep.maneuver
                val instruction = when {
                    currentDistance.contains("m") && nextManeuver.equals(
                        "turn-left",
                        ignoreCase = true
                    ) -> {
                        R.drawable.baseline_turn_left_24 to "Take Left turn"
                    }

                    currentDistance.contains("m") && nextManeuver.equals(
                        "turn-right",
                        ignoreCase = true
                    ) -> {
                        R.drawable.baseline_turn_right_24 to "Take Right turn"
                    }

                    else -> {
                        R.drawable.baseline_straight_24 to "Go Straight!"
                    }
                }
                Image(
                    painter = painterResource(id = instruction.first),
                    colorFilter = ColorFilter.tint(Color.White),
                    contentDescription = instruction.second,
                    modifier = Modifier.size(55.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = instruction.second,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Distance: $currentDistance",
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}