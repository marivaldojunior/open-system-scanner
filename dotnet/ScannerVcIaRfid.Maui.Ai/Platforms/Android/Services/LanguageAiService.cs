using System.Text.Json;
using Com.Scannervciarfid.AI.Language;
using ScannerVcIaRfid.Maui.Ai.Models;
using ScannerVcIaRfid.Maui.Ai.Services;

namespace ScannerVcIaRfid.Maui.Ai.Platforms.Android.Services;

public sealed class LanguageAiService : ILanguageAiService, IDisposable
{
    // O cliente nativo guarda os modelos do ML Kit em cache por idioma.
    private readonly LanguageAiClient _client = new();
    private bool _disposed;

    public Task<string> IdentifyLanguageAsync(string text) =>
        Run(listener => _client.IdentifyLanguage(text, listener));

    public Task<string> TranslateAsync(string text, string sourceLanguage, string targetLanguage, bool requireWifi = false) =>
        Run(listener => _client.Translate(text, sourceLanguage, targetLanguage, requireWifi, listener));

    public async Task<IReadOnlyList<TextEntity>> ExtractEntitiesAsync(string text, string languageTag = "pt")
    {
        var json = await Run(listener => _client.ExtractEntities(text, languageTag, listener));
        return JsonSerializer.Deserialize(json, AiJsonContext.Default.ListTextEntity) ?? [];
    }

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        _disposed = true;
        _client.Release();
        _client.Dispose();
    }

    // O cache de modelos do LanguageAiClient nao e thread-safe: todas as
    // chamadas passam pela main thread, onde o ML Kit tambem entrega os
    // resultados.
    private static Task<string> Run(Action<IAiTextListener> start)
    {
        var listener = new AiTextListener();
        MainThread.BeginInvokeOnMainThread(() => start(listener));
        return listener.Task;
    }

    /// <summary>
    /// Traduz os callbacks JNI de uma chamada em um Task.
    /// </summary>
    private sealed class AiTextListener : Java.Lang.Object, IAiTextListener
    {
        private readonly TaskCompletionSource<string> _completion =
            new(TaskCreationOptions.RunContinuationsAsynchronously);

        public Task<string> Task => _completion.Task;

        public void OnSuccess(string result) => _completion.TrySetResult(result);

        public void OnError(string error) => _completion.TrySetException(new InvalidOperationException(error));
    }
}
