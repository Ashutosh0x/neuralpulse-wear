package com.alphahealth.monitor.wear.surfaces

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

class EnergyScoreComplication : ComplicationDataSourceService() {

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        // Fetch current aggregated Energy Score (mock baseline = 78)
        val score = 78
        
        val complicationData = when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("$score").build(),
                    contentDescription = PlainComplicationText.Builder("Energy Score").build()
                )
                .setTitle(PlainComplicationText.Builder("ENG").build())
                .build()
            }
            else -> null
        }
        
        listener.onComplicationData(complicationData)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type == ComplicationType.SHORT_TEXT) {
            return ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("78").build(),
                contentDescription = PlainComplicationText.Builder("Energy Score Preview").build()
            )
            .setTitle(PlainComplicationText.Builder("ENG").build())
            .build()
        }
        return null
    }
}
