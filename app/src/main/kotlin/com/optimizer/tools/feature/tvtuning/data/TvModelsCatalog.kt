package com.optimizer.tools.feature.tvtuning.data

import com.optimizer.tools.feature.tvtuning.domain.TvModelProfile
import dev.adboptimizer.core.model.DeviceInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Listas curadas por modelo (de `assets/tv/tv-models.json`), importadas de CleanYourTV
 * con atribucion. Son un punto de partida: cada paquete pasa luego por los guards del
 * debloater antes de tocarse.
 */
class TvModelsCatalog(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun parse(raw: String): List<TvModelProfile> =
        runCatching { json.decodeFromString(TvModelsFile.serializer(), raw) }
            .getOrNull()
            ?.models
            ?.mapNotNull { model ->
                if (model.model.isBlank() && model.device.isBlank()) {
                    null
                } else {
                    TvModelProfile(
                        brand = model.brand,
                        model = model.model,
                        device = model.device,
                        boards = model.boards.filter { it.isNotBlank() },
                        sdk = model.sdk,
                        notes = model.notes,
                        bundles = model.bundles.mapValues { (_, packages) -> packages.distinct() },
                    )
                }
            }.orEmpty()

    fun match(
        models: List<TvModelProfile>,
        device: DeviceInfo,
    ): TvModelProfile? =
        matchModel(
            models = models,
            model = device.model,
            product = device.hardwareInfo?.product,
            board = device.hardwareInfo?.board,
        )

    companion object {
        /**
         * Coincidencia por `ro.product.model`, luego por `ro.product.name/device` y al
         * final por `ro.product.board`. Sin coincidencia no se aplica ningun bundle.
         */
        fun matchModel(
            models: List<TvModelProfile>,
            model: String,
            product: String?,
            board: String?,
        ): TvModelProfile? =
            models.firstOrNull { it.model.isNotBlank() && it.model.equals(model.trim(), ignoreCase = true) }
                ?: models.firstOrNull {
                    product != null && it.device.isNotBlank() && it.device.equals(product.trim(), ignoreCase = true)
                }
                ?: models.firstOrNull { profile ->
                    board != null && profile.boards.any { it.equals(board.trim(), ignoreCase = true) }
                }
    }
}

@Serializable
private data class TvModelsFile(
    val schemaVersion: Int = 1,
    val models: List<TvModelDto> = emptyList(),
)

@Serializable
private data class TvModelDto(
    val brand: String = "",
    val model: String = "",
    val device: String = "",
    val boards: List<String> = emptyList(),
    val sdk: Int? = null,
    val notes: String = "",
    val bundles: Map<String, List<String>> = emptyMap(),
)
