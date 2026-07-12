package com.vaia.presentation.ui.common

import android.content.Context
import androidx.annotation.DrawableRes
import com.vaia.R


private val DESTINATION_COVER_MAP = mapOf(
    "paris" to COVER_PARIS,
    "parís" to COVER_PARIS,
    "london" to COVER_LONDON,
    "londres" to COVER_LONDON,
    "rome" to COVER_ROME,
    "roma" to COVER_ROME,
    "madrid" to COVER_MADRID,
    "barcelona" to COVER_BARCELONA,
    "new york" to COVER_NEW_YORK,
    "nueva york" to COVER_NEW_YORK,
    "bali" to COVER_BALI,
    "tokyo" to COVER_TOKYO,
    "tokio" to COVER_TOKYO,
    "bangkok" to COVER_BANGKOK,
    "amsterdam" to COVER_AMSTERDAM,
    "berlin" to COVER_BERLIN,
    "berlín" to COVER_BERLIN,
    "lisboa" to COVER_LISBOA,
    "praga" to COVER_PRAGA,
    "atenas" to COVER_ATENAS,
    "sydney" to COVER_SYDNEY,
    "sídney" to COVER_SYDNEY,
    "dubai" to COVER_DUBAI,
    "dubái" to COVER_DUBAI,
    "egypt" to COVER_EGYPT,
    "egipto" to COVER_EGYPT,
    "morocco" to COVER_MOROCCO,
    "marruecos" to COVER_MOROCCO,
    "peru" to COVER_PERU,
    "perú" to COVER_PERU,
    "argentina" to COVER_ARGENTINA,
    "brasil" to COVER_BRASIL,
    "brazil" to COVER_BRASIL,
    "méxico" to COVER_MEXICO,
    "mexico" to COVER_MEXICO,
)

private const val COVER_PARIS = "https://images.unsplash.com/photo-1431274172761-fca41d930114?w=1200&q=80&auto=format&fit=crop"
private const val COVER_LONDON = "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=1200&q=80&auto=format&fit=crop"
private const val COVER_ROME = "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=1200&q=80&auto=format&fit=crop"
private const val COVER_MADRID = "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=1200&q=80&auto=format&fit=crop"
private const val COVER_BARCELONA = "https://images.unsplash.com/photo-1583422409516-2895a77efded?w=1200&q=80&auto=format&fit=crop"
private const val COVER_NEW_YORK = "https://images.unsplash.com/photo-1499092346589-b9b6be3e94b2?w=1200&q=80&auto=format&fit=crop"
private const val COVER_BALI = "https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=1200&q=80&auto=format&fit=crop"
private const val COVER_TOKYO = "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=1200&q=80&auto=format&fit=crop"
private const val COVER_BANGKOK = "https://images.unsplash.com/photo-1508009603885-50cf7c579365?w=1200&q=80&auto=format&fit=crop"
private const val COVER_AMSTERDAM = "https://images.unsplash.com/photo-1534351590666-13e3e96b5017?w=1200&q=80&auto=format&fit=crop"
private const val COVER_BERLIN = "https://images.unsplash.com/photo-1560969184-10fe8719e047?w=1200&q=80&auto=format&fit=crop"
private const val COVER_LISBOA = "https://images.unsplash.com/photo-1585208798174-6cedd86e019a?w=1200&q=80&auto=format&fit=crop"
private const val COVER_PRAGA = "https://images.unsplash.com/photo-1541849546-216549ae216d?w=1200&q=80&auto=format&fit=crop"
private const val COVER_ATENAS = "https://images.unsplash.com/photo-1555993539-1732b0258235?w=1200&q=80&auto=format&fit=crop"
private const val COVER_SYDNEY = "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?w=1200&q=80&auto=format&fit=crop"
private const val COVER_DUBAI = "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=1200&q=80&auto=format&fit=crop"
private const val COVER_EGYPT = "https://images.unsplash.com/photo-1539768942893-daf53e736b68?w=1200&q=80&auto=format&fit=crop"
private const val COVER_MOROCCO = "https://images.unsplash.com/photo-1518002171953-a080ee817e1f?w=1200&q=80&auto=format&fit=crop"
private const val COVER_PERU = "https://images.unsplash.com/photo-1526392060635-9d6019884377?w=1200&q=80&auto=format&fit=crop"
private const val COVER_ARGENTINA = "https://images.unsplash.com/photo-1589909202802-8f4aadce1849?w=1200&q=80&auto=format&fit=crop"
private const val COVER_BRASIL = "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?w=1200&q=80&auto=format&fit=crop"
private const val COVER_MEXICO = "https://images.unsplash.com/photo-1585464231875-d9ef1f5ad396?w=1200&q=80&auto=format&fit=crop"

private val DEFAULT_COVER_URL =
    "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=1200&q=80&auto=format&fit=crop"

fun tripCoverImageUrl(destination: String): String {
    val key = destination.lowercase()
    return DESTINATION_COVER_MAP.entries.firstOrNull { it.key in key }?.value
        ?: DEFAULT_COVER_URL
}

enum class TripCoverPreset(val url: String, val label: String) {
    LANDSCAPE("https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=1200&q=80&auto=format&fit=crop", "Paisaje"),
    BEACH("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80&auto=format&fit=crop", "Playa"),
    CITY("https://images.unsplash.com/photo-1449824913935-59a10b8d2000?w=1200&q=80&auto=format&fit=crop", "Ciudad"),
    MOUNTAINS("https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1200&q=80&auto=format&fit=crop", "Montañas"),
    CULTURE("https://images.unsplash.com/photo-1533105079780-92b9be482077?w=1200&q=80&auto=format&fit=crop", "Cultura"),
}

@DrawableRes
fun tripCoverPlaceholderRes(): Int = R.drawable.ic_trip_cover_placeholder

fun getTripCoverUrl(context: Context, tripId: String, destination: String): String {
    val prefs = context.getSharedPreferences("trip_covers", Context.MODE_PRIVATE)
    return prefs.getString(tripId, null) ?: tripCoverImageUrl(destination)
}

