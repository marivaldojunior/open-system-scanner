using Microsoft.Extensions.Logging;
using ScannerVcIaRfid.Maui.Controls;
using ScannerVcIaRfid.Maui.Services;

#if ANDROID
using ScannerVcIaRfid.Maui.Platforms.Android.Handlers;
using ScannerVcIaRfid.Maui.Platforms.Android.Services;
#endif

namespace ScannerVcIaRfid.Maui.Sample;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();

        builder.UseMauiApp<App>();

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
#endif

        builder.Services.AddTransient<MainPage>();

#if DEBUG
        builder.Logging.AddDebug();
#endif

        return builder.Build();
    }
}
