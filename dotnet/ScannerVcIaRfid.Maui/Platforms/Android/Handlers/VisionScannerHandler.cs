using Com.Scannervciarfid.Vision;
using Com.Scannervciarfid.Vision.Barcode;
using Com.Scannervciarfid.Vision.Ocr;
using Microsoft.Maui.Handlers;
using ScannerVcIaRfid.Maui.Controls;
using ScannerVcIaRfid.Maui.Models;
using ScannerVcIaRfid.Maui.Platforms.Android.Native;
using ScanErrorEventArgs = ScannerVcIaRfid.Maui.Models.ScanErrorEventArgs;

namespace ScannerVcIaRfid.Maui.Platforms.Android.Handlers;

public sealed class VisionScannerHandler : ViewHandler<VisionScanner, VisionScannerView>
{
    public static readonly IPropertyMapper<VisionScanner, VisionScannerHandler> Mapper =
        new PropertyMapper<VisionScanner, VisionScannerHandler>(ViewMapper)
        {
            [nameof(VisionScanner.Engine)] = MapEngine
        };

    // Strict gating: StartCamera() no VisionScanner nao toca a view nativa
    // diretamente, ele so faz Handler?.Invoke(nameof(StartCamera)). Este
    // CommandMapper e quem de fato traduz esse comando em uma chamada
    // nativa - e so dispara quando o consumidor mandar explicitamente.
    public static readonly CommandMapper<VisionScanner, VisionScannerHandler> CommandMapper =
        new(ViewCommandMapper)
        {
            [nameof(VisionScanner.StartCamera)] = MapStartCamera
        };

    private ScannerListenerWrapper? _listenerWrapper;

    static VisionScannerHandler()
    {
        // Os motores vivem em modulos nativos separados (vision-barcode,
        // vision-ocr). Cada um se registra no VisionEngineRegistry, e o
        // MapEngine abaixo escolhe pelo id recebido do VisionScanner.Engine.
        BarcodeEngine.Register();
        OcrTextEngine.Register();
    }

    public VisionScannerHandler() : base(Mapper, CommandMapper)
    {
    }

    private static void MapEngine(VisionScannerHandler handler, VisionScanner view)
    {
        var engine = VisionEngineRegistry.Create(view.Engine);
        if (engine is null)
        {
            view.RaiseScanError(
                $"Motor de visao '{view.Engine}' nao registrado no VisionEngineRegistry nativo.");
            return;
        }

        handler.PlatformView.SetVisionEngine(engine);
    }

    private static void MapStartCamera(VisionScannerHandler handler, VisionScanner view, object? args)
    {
        handler.PlatformView.StartCamera();
    }

    protected override VisionScannerView CreatePlatformView()
    {
        // O motor e definido pelo MapEngine, que roda logo apos o
        // ConnectHandler (e de novo a cada mudanca de VisionScanner.Engine).
        return new VisionScannerView(Context);
    }

    protected override void ConnectHandler(VisionScannerView platformView)
    {
        base.ConnectHandler(platformView);

        _listenerWrapper = new ScannerListenerWrapper();
        _listenerWrapper.ScanSucceeded += OnScanSucceeded;
        _listenerWrapper.ScanFailed += OnScanFailed;

        // Nao chamamos platformView.StartCamera() aqui de proposito (strict
        // gating) - a captura so deve comecar quando o consumidor mandar via
        // VisionScanner.StartCamera(), tipicamente apos confirmar a
        // permissao de camera em runtime (ver MainPage.OnAppearing).
        platformView.SetScannerListener(_listenerWrapper);
    }

    protected override void DisconnectHandler(VisionScannerView platformView)
    {
        platformView.SetScannerListener(null);

        if (_listenerWrapper is not null)
        {
            _listenerWrapper.ScanSucceeded -= OnScanSucceeded;
            _listenerWrapper.ScanFailed -= OnScanFailed;
            _listenerWrapper.Dispose();
            _listenerWrapper = null;
        }

        base.DisconnectHandler(platformView);
    }

    private void OnScanSucceeded(object? sender, ScanResultEventArgs e)
    {
        VirtualView?.RaiseScanned(e.Result);
    }

    private void OnScanFailed(object? sender, ScanErrorEventArgs e)
    {
        VirtualView?.RaiseScanError(e.Message);
    }
}
