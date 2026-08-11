using Microsoft.Extensions.Logging;
using OpenSystemScanner.Maui.Controls;
using OpenSystemScanner.Maui.Services;

#if ANDROID
using OpenSystemScanner.Maui.Platforms.Android.Handlers;
using OpenSystemScanner.Maui.Platforms.Android.Services;
#endif

namespace OpenSystemScanner.Maui.Sample;

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
#endif

        builder.Services.AddTransient<MainPage>();

#if DEBUG
        builder.Logging.AddDebug();
#endif

        return builder.Build();
    }
}
