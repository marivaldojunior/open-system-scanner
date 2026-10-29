using Microsoft.Extensions.Logging;
using ScannerVcIaRfid.Maui.Ai.Services;
using ScannerVcIaRfid.Maui.Controls;
using ScannerVcIaRfid.Maui.Nfc.Services;
using ScannerVcIaRfid.Maui.OcrScripts;
using ScannerVcIaRfid.Maui.Rfid.Services;
using ScannerVcIaRfid.Maui.Services;
using ScannerVcIaRfid.Maui.VisionExtras;

#if ANDROID
using ScannerVcIaRfid.Maui.Ai.Platforms.Android.Services;
using ScannerVcIaRfid.Maui.Platforms.Android.Handlers;
using ScannerVcIaRfid.Maui.Nfc.Platforms.Android.Services;
using ScannerVcIaRfid.Maui.Platforms.Android.Services;
using ScannerVcIaRfid.Maui.Rfid.Platforms.Android.Services;
#endif

namespace ScannerVcIaRfid.Maui.Sample;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();

        builder.UseMauiApp<App>();

        // Motores extras (objetos, rotulos, rostos) para o VisionScanner.
        builder.UseVisionExtras();

        // OCR em chines, japones, coreano e devanagari.
        builder.UseOcrScripts();

        builder.ConfigureMauiHandlers(handlers =>
        {
#if ANDROID
            handlers.AddHandler<VisionScanner, VisionScannerHandler>();
#endif
        });

        // IHardwareScannerService representa o leitor fisico do aparelho -
        // um recurso global do dispositivo, nao algo por-pagina ou
        // por-navegacao. Singleton garante uma unica instancia/listener
        // durante todo o ciclo de vida do app.
#if ANDROID
        builder.Services.AddSingleton<IHardwareScannerService, HardwareScannerService>();
        builder.Services.AddSingleton<ICodeScannerService, CodeScannerService>();
        builder.Services.AddSingleton<IDocumentScannerService, DocumentScannerService>();
        builder.Services.AddSingleton<INfcService, NfcService>();
        builder.Services.AddSingleton<IRfidReaderService, RfidReaderService>();
        builder.Services.AddSingleton<ILanguageAiService, LanguageAiService>();
#endif

        builder.Services.AddTransient<MainPage>();

#if DEBUG
        builder.Logging.AddDebug();
#endif

        return builder.Build();
    }
}
