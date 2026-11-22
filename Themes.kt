package com.prokey.computerkeyboard

data class KbTheme(
    val id: String,
    val name: String,
    val bg: Int,
    val bgEnd: Int?,
    val key: Int,
    val keySpecial: Int,
    val keyPressed: Int,
    val text: Int,
    val accent: Int,
    val border: Int = 0,
)

object Themes {
    private fun c(rgb: Int) = 0xFF000000.toInt() or rgb
    private fun a(alpha: Int, rgb: Int) = (alpha shl 24) or rgb

    val all: List<KbTheme> = listOf(
        KbTheme("classic_black", "Classic Black", c(0x0B0B0C), null, c(0x2A2A2D), c(0x1B1B1D), c(0x4A4A50), c(0xF2F2F2), c(0x3D7BFF)),
        KbTheme("pure_white", "Pure White", c(0xF1F3F4), null, c(0xFFFFFF), c(0xD9DCE0), c(0xBFC4CA), c(0x1B1C1E), c(0x2F6BFF), c(0xC9CDD2)),
        KbTheme("dark_pro", "Dark Pro", c(0x1E2128), null, c(0x2D323B), c(0x23272E), c(0x434A57), c(0xE6E9EF), c(0x4C8DFF)),
        KbTheme("blue_neon", "Blue Neon", c(0x050A18), null, c(0x0B1630), c(0x08112A), c(0x16306B), c(0xBFE3FF), c(0x00B7FF), c(0x00B7FF)),
        KbTheme("purple_neon", "Purple Neon", c(0x0D0618), null, c(0x1A0F2E), c(0x130A24), c(0x3A1E66), c(0xF0D9FF), c(0xC04BFF), c(0xC04BFF)),
        KbTheme("green_neon", "Green Neon", c(0x03100A), null, c(0x08241A), c(0x061A12), c(0x0F4A33), c(0xCFFFE5), c(0x1CFF8C), c(0x1CFF8C)),
        KbTheme("red_gaming", "Red Gaming", c(0x0E0506), null, c(0x241012), c(0x180A0B), c(0x5A1A1F), c(0xFFE3E3), c(0xFF2D3B), c(0xFF2D3B)),
        KbTheme("cyberpunk", "Cyberpunk", c(0x120A24), c(0x2A0A3D), c(0x1D1B3A), c(0x141230), c(0x4A2A7A), c(0xFFF200), c(0xFF2BD6), c(0x00F0FF)),
        KbTheme("amoled_black", "AMOLED Black", c(0x000000), null, c(0x121212), c(0x0A0A0A), c(0x2A2A2A), c(0xFFFFFF), c(0x3D7BFF)),
        KbTheme("glass", "Glass Style", c(0x2B3A55), c(0x6C7FA8), a(0x55, 0xFFFFFF), a(0x33, 0xFFFFFF), a(0x88, 0xFFFFFF), c(0xFFFFFF), c(0x4C8DFF), a(0x66, 0xFFFFFF)),
        KbTheme("gradient", "Gradient", c(0x1A2980), c(0x26D0CE), a(0x44, 0xFFFFFF), a(0x2A, 0xFFFFFF), a(0x77, 0xFFFFFF), c(0xFFFFFF), c(0xFF6B6B)),
        KbTheme("minimal", "Minimal", c(0xEDEDED), null, c(0xF7F7F7), c(0xE4E4E4), c(0xCFCFCF), c(0x333333), c(0x333333)),
        KbTheme("ocean_blue", "Ocean Blue", c(0x0A3D62), c(0x0C2A4A), c(0x1E5F8F), c(0x154A73), c(0x3C8DC4), c(0xFFFFFF), c(0x00D2FF)),
        KbTheme("gold_premium", "Gold Premium", c(0x121212), null, c(0x1E1B14), c(0x16140E), c(0x4A3F1E), c(0xF5D77A), c(0xD4AF37), c(0x8C6D1F)),
        KbTheme("computer_classic", "Computer Classic", c(0xA9A79A), null, c(0xDCD8C6), c(0xBDB9A6), c(0x9B977F), c(0x222222), c(0x2F5D8C), c(0x7C7A6B)),
    )

    fun byId(id: String): KbTheme = all.firstOrNull { it.id == id } ?: all[0]
}
