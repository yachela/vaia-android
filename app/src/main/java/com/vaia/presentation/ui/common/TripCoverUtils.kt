package com.vaia.presentation.ui.common

import androidx.annotation.DrawableRes
import com.vaia.R

private val DESTINATION_COVER_MAP = mapOf(
    "paris" to "https://images.unsplash.com/photo-1431274172761-fca41d930114?w=1200&q=80&auto=format&fit=crop",
    "parís" to "https://images.unsplash.com/photo-1431274172761-fca41d930114?w=1200&q=80&auto=format&fit=crop",
    "london" to "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=1200&q=80&auto=format&fit=crop",
    "londres" to "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=1200&q=80&auto=format&fit=crop",
    "rome" to "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=1200&q=80&auto=format&fit=crop",
    "roma" to "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=1200&q=80&auto=format&fit=crop",
    "madrid" to "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=1200&q=80&auto=format&fit=crop",
    "barcelona" to "https://images.unsplash.com/photo-1583422409516-2895a77efded?w=1200&q=80&auto=format&fit=crop",
    "new york" to "https://images.unsplash.com/photo-1499092346589-b9b6be3e94b2?w=1200&q=80&auto=format&fit=crop",
    "nueva york" to "https://images.unsplash.com/photo-1499092346589-b9b6be3e94b2?w=1200&q=80&auto=format&fit=crop",
    "bali" to "https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=1200&q=80&auto=format&fit=crop",
    "tokyo" to "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=1200&q=80&auto=format&fit=crop",
    "tokio" to "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=1200&q=80&auto=format&fit=crop",
    "bangkok" to "https://images.unsplash.com/photo-1508009603885-50cf7c579365?w=1200&q=80&auto=format&fit=crop",
    "amsterdam" to "https://images.unsplash.com/photo-1534351590666-13e3e96b5017?w=1200&q=80&auto=format&fit=crop",
    "berlin" to "https://images.unsplash.com/photo-1560969184-10fe8719e047?w=1200&q=80&auto=format&fit=crop",
    "berlín" to "https://images.unsplash.com/photo-1560969184-10fe8719e047?w=1200&q=80&auto=format&fit=crop",
    "lisboa" to "https://images.unsplash.com/photo-1585208798174-6cedd86e019a?w=1200&q=80&auto=format&fit=crop",
    "praga" to "https://images.unsplash.com/photo-1541849546-216549ae216d?w=1200&q=80&auto=format&fit=crop",
    "atenas" to "https://images.unsplash.com/photo-1555993539-1732b0258235?w=1200&q=80&auto=format&fit=crop",
    "sydney" to "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?w=1200&q=80&auto=format&fit=crop",
    "sídney" to "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?w=1200&q=80&auto=format&fit=crop",
    "dubai" to "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=1200&q=80&auto=format&fit=crop",
    "dubái" to "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=1200&q=80&auto=format&fit=crop",
    "egypt" to "https://images.unsplash.com/photo-1539768942893-daf53e736b68?w=1200&q=80&auto=format&fit=crop",
    "egipto" to "https://images.unsplash.com/photo-1539768942893-daf53e736b68?w=1200&q=80&auto=format&fit=crop",
    "morocco" to "https://images.unsplash.com/photo-1518002171953-a080ee817e1f?w=1200&q=80&auto=format&fit=crop",
    "marruecos" to "https://images.unsplash.com/photo-1518002171953-a080ee817e1f?w=1200&q=80&auto=format&fit=crop",
    "peru" to "https://images.unsplash.com/photo-1526392060635-9d6019884377?w=1200&q=80&auto=format&fit=crop",
    "perú" to "https://images.unsplash.com/photo-1526392060635-9d6019884377?w=1200&q=80&auto=format&fit=crop",
    "argentina" to "https://images.unsplash.com/photo-1589909202802-8f4aadce1849?w=1200&q=80&auto=format&fit=crop",
    "brasil" to "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?w=1200&q=80&auto=format&fit=crop",
    "brazil" to "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?w=1200&q=80&auto=format&fit=crop",
    "méxico" to "https://images.unsplash.com/photo-1585464231875-d9ef1f5ad396?w=1200&q=80&auto=format&fit=crop",
    "mexico" to "https://images.unsplash.com/photo-1585464231875-d9ef1f5ad396?w=1200&q=80&auto=format&fit=crop",
)

private val DEFAULT_COVER_URL =
    "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=1200&q=80&auto=format&fit=crop"

fun tripCoverImageUrl(destination: String): String {
    val key = destination.lowercase()
    return DESTINATION_COVER_MAP.entries.firstOrNull { it.key in key }?.value
        ?: DEFAULT_COVER_URL
}

enum class TripCoverPreset(@DrawableRes val drawableRes: Int, val label: String) {
    MOUNTAINS(R.drawable.ic_trip_cover_mountains, "Montañas"),
    BEACH(R.drawable.ic_trip_cover_beach, "Playa"),
    CITY(R.drawable.ic_trip_cover_city, "Ciudad"),
    NATURE(R.drawable.ic_trip_cover_nature, "Naturaleza"),
    CULTURE(R.drawable.ic_trip_cover_culture, "Cultura"),
}

val TRIP_COVER_PLACEHOLDER_RES: Int
    @DrawableRes get() = R.drawable.ic_trip_cover_placeholder
