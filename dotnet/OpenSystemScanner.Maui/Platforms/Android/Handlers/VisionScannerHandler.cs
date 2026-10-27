using Com.Opensystemscanner;
using Com.Opensystemscanner.Core;
using Com.Opensystemscanner.Vision;
using Microsoft.Maui.Handlers;
using OpenSystemScanner.Maui.Controls;
using OpenSystemScanner.Maui.Models;
using OpenSystemScanner.Maui.Platforms.Android.Native;
using ScanErrorEventArgs = OpenSystemScanner.Maui.Models.ScanErrorEventArgs;

namespace OpenSystemScanner.Maui.Platforms.Android.Handlers;

public sealed class VisionScannerHandler : ViewHandler<VisionScanner, VisionScannerView>
{
    public static readonly IPropertyMapper<VisionScanner, VisionScannerHandler> Mapper =
        new PropertyMapper<VisionScanner, VisionScannerHandler>(ViewMapper);

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

    public VisionScannerHandler() : base(Mapper, CommandMapper)
    {
    }

    private static void MapStartCamera(VisionScannerHandler handler, VisionScanner view, object? args)
    {
        handler.PlatformView.StartCamera();
    }

    protected override VisionScannerView CreatePlatformView()
    {
        // CaptureMode.CameraBarcode depende do rename feito em
        // OpenSystemScanner.Android.Binding/Transforms/Metadata.xml.
        // Sem a transformacao, use CaptureMode.CAMERA_BARCODE.
        return DataCaptureFactory.GetCameraScannerView(Context, CaptureMode.CameraBarcode);
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
