using ScannerVcIaRfid.Maui.Ai.Models;

namespace ScannerVcIaRfid.Maui.Ai.Services;

/// <summary>
/// IA de linguagem on-device (ML Kit). O texto nao sai do aparelho. Traducao
/// e extracao de entidades baixam o modelo do idioma na primeira chamada
/// (precisa de rede uma vez). Falhas lancam <see cref="InvalidOperationException"/>.
/// </summary>
public interface ILanguageAiService
{
    /// <summary>Codigo BCP-47 do idioma (ex.: "pt") ou "und" se nao reconhecido.</summary>
    Task<string> IdentifyLanguageAsync(string text);

    /// <param name="requireWifi">Baixa o modelo (~30 MB por idioma) so em Wi-Fi.</param>
    Task<string> TranslateAsync(string text, string sourceLanguage, string targetLanguage, bool requireWifi = false);

    /// <summary>
    /// Encontra datas, enderecos, telefones, e-mails, valores, numeros de
    /// rastreio e outros dados no texto (ex.: resultado do OCR).
    /// </summary>
    Task<IReadOnlyList<TextEntity>> ExtractEntitiesAsync(string text, string languageTag = "pt");
}
