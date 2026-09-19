package com.tvsencilla.iptv.domain.model

/** Content text never goes below 32sp, so even "Normal" is a large scale. */
enum class FontSizeOption(val scale: Float) {
    NORMAL(1.0f),
    LARGE(1.2f),
    XLARGE(1.45f),
}

enum class SubtitleSizeOption(val scale: Float) {
    NORMAL(1.0f),
    LARGE(1.3f),
    XLARGE(1.6f),
}

/**
 * Which numbering the remote's number keys and CH+/CH- follow.
 * Never both at once, as the prompt requires.
 */
enum class NumberingMode { FAVORITES, FULL_LIST }

data class AppSettings(
    val setupCompleted: Boolean = false,
    val simpleMode: Boolean = true,
    val fontSize: FontSizeOption = FontSizeOption.NORMAL,
    val subtitleSize: SubtitleSizeOption = SubtitleSizeOption.LARGE,
    val numberingMode: NumberingMode = NumberingMode.FAVORITES,
    val startOnLastChannel: Boolean = false,
    val showEpgGrid: Boolean = false,
    /** Al pedir un canal por voz, ponerlo directamente en lugar de enseñar la lista. */
    val voiceAutoTune: Boolean = true,
    val preferredAudioLanguage: String = "es",
    val preferredSubtitleLanguage: String = "es",
    val pin: String = DEFAULT_PIN,
    val hiddenCategoryIds: Set<String> = emptySet(),
    val lastChannelId: String? = null,
) {
    companion object {
        const val DEFAULT_PIN = "0000"
    }
}
