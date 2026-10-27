using System.Diagnostics;
using System.Text.Json;
using ScannerVcIaRfid.Maui.Controls;
using ScannerVcIaRfid.Maui.Models;
using ScannerVcIaRfid.Maui.Services;

namespace ScannerVcIaRfid.Maui.Sample;

public partial class MainPage : ContentPage
{
    private readonly IHardwareScannerService _hardwareScanner;

    public MainPage(IHardwareScannerService hardwareScanner)
    {
        InitializeComponent();

        _hardwareScanner = hardwareScanner;
    }

    protected override async void OnAppearing()
    {
        base.OnAppearing();

        // Initialize() e idempotente do lado do servico (ver
        // HardwareScannerService.Initialize) - seguro chamar toda vez que a
        // pagina reaparece apos navegacao. O leitor fisico nao depende de
        // permissao de runtime (chega via Intent do sistema/DataWedge), soh
        // a camera exige.
        _hardwareScanner.Initialize();
        _hardwareScanner.ScanSucceeded += OnHardwareScanSucceeded;
        _hardwareScanner.ScanFailed += OnHardwareScanFailed;
        _hardwareScanner.Start();

        CameraScannerView.Scanned += OnCameraScanned;
        CameraScannerView.ScanError += OnCameraScanError;

        var cameraGranted = await EnsureCameraPermissionAsync();
        if (cameraGranted)
        {
            // Strict gating: o VisionScannerHandler nao inicia a camera
            // sozinho ao conectar (ver ConnectHandler) - so chamamos
            // StartCamera() explicitamente aqui, e somente depois de termos
            // a confirmacao da permissao em runtime. Isso elimina a
            // condicao de corrida entre o bind do CameraX e o dialogo de
            // permissao do usuario.
            CameraScannerView.StartCamera();
        }
        else
        {
            // Mesmo sem esse guard, VisionScannerView.bindUseCases() ja
            // captura SecurityException num try/catch e reporta via
            // onScanError - nao ha risco de crash. O guard existe pela UX:
            // sem ele a camera falharia "silenciosamente" (preview preto,
            // nenhuma leitura), sem o usuario entender o motivo.
            CameraScannerView.IsVisible = false;
            LastScanLabel.Text = "Permissão de câmera negada — leitura via câmera desativada.";
        }
    }

    private static async Task<bool> EnsureCameraPermissionAsync()
    {
        var status = await Permissions.CheckStatusAsync<Permissions.Camera>();

        if (status != PermissionStatus.Granted)
        {
            status = await Permissions.RequestAsync<Permissions.Camera>();
        }

        return status == PermissionStatus.Granted;
    }

    protected override void OnDisappearing()
    {
        base.OnDisappearing();

        _hardwareScanner.ScanSucceeded -= OnHardwareScanSucceeded;
        _hardwareScanner.ScanFailed -= OnHardwareScanFailed;
        _hardwareScanner.Stop();

        CameraScannerView.Scanned -= OnCameraScanned;
        CameraScannerView.ScanError -= OnCameraScanError;

        // Release() nao e chamado aqui de proposito: o leitor de hardware e
        // um Singleton compartilhado pelo app inteiro. Desregistrar o
        // BroadcastReceiver so acontece quando o proprio servico Singleton e
        // descartado (encerramento do app), nao a cada saida desta pagina.
    }

    private void OnHardwareScanSucceeded(object? sender, ScanResultEventArgs e) => HandleScanResult(e.Result);

    private void OnHardwareScanFailed(object? sender, ScanErrorEventArgs e) => HandleScanError(e.Message);

    private void OnCameraScanned(object? sender, ScanResultEventArgs e) => HandleScanResult(e.Result);

    private void OnCameraScanError(object? sender, ScanErrorEventArgs e) => HandleScanError(e.Message);

    private void HandleScanResult(ScanResult result)
    {
        // Os callbacks do leitor de hardware (BroadcastReceiver via JNI) e da
        // camera (pipeline de analise do CameraX/ML Kit) nao tem garantia de
        // chegar na main thread. Qualquer toque em elemento de UI precisa
        // passar por MainThread.BeginInvokeOnMainThread, senao o MAUI lanca
        // excecao de concorrencia de thread.
        MainThread.BeginInvokeOnMainThread(() =>
        {
            LastScanLabel.Text = result.Format == "VISION_OCR"
                ? FormatOcrSummary(result.Value)
                : $"{result.Value}  ({result.Format} via {result.SourceEngine})";
        });
    }

    private static string FormatOcrSummary(string rawJson)
    {
        try
        {
            var payload = JsonSerializer.Deserialize<OcrPayload>(rawJson);
            if (payload is null)
            {
                return "OCR: payload vazio.";
            }

            // Substring(0, 20) direto quebraria em textos com menos de 20
            // caracteres (ArgumentOutOfRangeException) - truncamento seguro.
            var preview = payload.FullText.Length > 20
                ? payload.FullText[..20]
                : payload.FullText;

            return $"OCR: {preview}... ({payload.Blocks.Count} blocos encontrados)";
        }
        catch (JsonException ex)
        {
            Debug.WriteLine($"[ScannerVcIaRfid] Falha ao desserializar payload OCR: {ex.Message}");
            return "OCR: falha ao interpretar o resultado.";
        }
    }

    private void HandleScanError(string message)
    {
        MainThread.BeginInvokeOnMainThread(() =>
        {
            Debug.WriteLine($"[ScannerVcIaRfid] Scan error: {message}");
        });
    }

    private void OnActivateCameraClicked(object? sender, EventArgs e)
    {
        CameraScannerView.IsVisible = !CameraScannerView.IsVisible;
        ActivateCameraButton.Text = CameraScannerView.IsVisible ? "Ocultar Câmera" : "Ativar Câmera";

        if (CameraScannerView.IsVisible)
        {
            // StartCamera() e seguro chamar mais de uma vez - internamente
            // refaz unbindAll()/bindToLifecycle() no CameraX. Reforcamos
            // aqui para o caso de o usuario reexibir o preview depois de
            // conceder a permissao numa segunda tentativa (ver
            // EnsureCameraPermissionAsync/OnAppearing).
            CameraScannerView.StartCamera();
        }
    }

    private void OnSwitchEngineClicked(object? sender, EventArgs e)
    {
        // Engine e uma BindableProperty: o handler troca o motor nativo na
        // hora, mesmo com a camera ligada.
        var useOcr = CameraScannerView.Engine != VisionEngines.Ocr;
        CameraScannerView.Engine = useOcr ? VisionEngines.Ocr : VisionEngines.Barcode;
        SwitchEngineButton.Text = useOcr ? "Motor: OCR" : "Motor: Código de barras";
    }
}
