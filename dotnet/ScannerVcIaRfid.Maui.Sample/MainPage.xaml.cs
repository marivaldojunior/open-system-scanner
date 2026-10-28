using System.Diagnostics;
using System.Text.Json;
using ScannerVcIaRfid.Maui.Ai.Services;
using ScannerVcIaRfid.Maui.Controls;
using ScannerVcIaRfid.Maui.Models;
using ScannerVcIaRfid.Maui.Nfc.Models;
using ScannerVcIaRfid.Maui.Nfc.Services;
using ScannerVcIaRfid.Maui.Rfid.Models;
using ScannerVcIaRfid.Maui.Rfid.Services;
using ScannerVcIaRfid.Maui.Services;
using ScannerVcIaRfid.Maui.VisionExtras;

namespace ScannerVcIaRfid.Maui.Sample;

public partial class MainPage : ContentPage
{
    private readonly IHardwareScannerService _hardwareScanner;
    private readonly ICodeScannerService _codeScanner;
    private readonly IDocumentScannerService _documentScanner;
    private readonly INfcService _nfc;
    private readonly IRfidReaderService _rfid;
    private readonly ILanguageAiService _languageAi;
    private string? _lastReadText;
    private readonly HashSet<string> _inventoryEpcs = [];
    private string? _lastEpc;
    private bool _inventoryRunning;

    public MainPage(
        IHardwareScannerService hardwareScanner,
        ICodeScannerService codeScanner,
        IDocumentScannerService documentScanner,
        INfcService nfc,
        IRfidReaderService rfid,
        ILanguageAiService languageAi)
    {
        InitializeComponent();

        _hardwareScanner = hardwareScanner;
        _codeScanner = codeScanner;
        _documentScanner = documentScanner;
        _nfc = nfc;
        _rfid = rfid;
        _languageAi = languageAi;
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

        if (_rfid.IsAvailable)
        {
            _rfid.Initialize();
            _rfid.TagsRead += OnRfidTagsRead;
            _rfid.RfidError += OnRfidError;
        }
        else
        {
            RfidInventoryButton.IsEnabled = false;
            RfidLabel.Text = "Este aparelho não tem leitor RFID suportado.";
        }

        // NFC nao precisa de permissao em runtime (permissao normal).
        if (_nfc.IsSupported)
        {
            _nfc.TagRead += OnNfcTagRead;
            _nfc.NfcError += OnNfcError;
            _nfc.Start();
        }
        else
        {
            LastNfcLabel.Text = "Este aparelho não tem NFC.";
        }

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

        _nfc.TagRead -= OnNfcTagRead;
        _nfc.NfcError -= OnNfcError;
        _nfc.Stop();

        if (_inventoryRunning)
        {
            _rfid.StopInventory();
            SetInventoryRunning(false);
        }
        _rfid.TagsRead -= OnRfidTagsRead;
        _rfid.RfidError -= OnRfidError;

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
            _lastReadText = result.Format == "VISION_OCR" ? ReadOcrFullText(result.Value) : result.Value;
            LastScanLabel.Text = result.Format == "VISION_OCR"
                ? FormatOcrSummary(result.Value)
                : $"{result.Value}  ({result.Format} via {result.SourceEngine})";
        });
    }

    private static string? ReadOcrFullText(string rawJson)
    {
        try
        {
            return JsonSerializer.Deserialize<OcrPayload>(rawJson)?.FullText;
        }
        catch (JsonException)
        {
            return null;
        }
    }

    // Exemplo de IA sobre a captura: o texto do OCR vira dados estruturados
    // (datas, telefones, valores, rastreios...) sem sair do aparelho.
    private async void OnExtractEntitiesClicked(object? sender, EventArgs e)
    {
        var text = _lastReadText;
        if (string.IsNullOrWhiteSpace(text))
        {
            AiLabel.Text = "Nenhum texto lido ainda.";
            return;
        }

        try
        {
            AiLabel.Text = "Analisando...";
            var language = await _languageAi.IdentifyLanguageAsync(text);
            var entities = await _languageAi.ExtractEntitiesAsync(text, language == "und" ? "pt" : language);

            AiLabel.Text = entities.Count == 0
                ? $"Idioma: {language}. Nenhuma entidade encontrada."
                : $"Idioma: {language}. " + string.Join("; ", entities.Select(
                    entity => $"{entity.Entities.FirstOrDefault()?.Type}: {entity.Text}"));
        }
        catch (InvalidOperationException ex)
        {
            AiLabel.Text = ex.Message;
        }
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

    private void OnNfcTagRead(object? sender, NfcTagEventArgs e)
    {
        // O texto do 1o registro NDEF, quando existe, e mais util que o UID.
        var firstRecord = e.Tag.NdefRecords.FirstOrDefault();
        var content = firstRecord?.Text ?? firstRecord?.Uri;

        MainThread.BeginInvokeOnMainThread(() =>
        {
            LastNfcLabel.Text = content is null
                ? $"NFC {e.Tag.Uid} ({string.Join(", ", e.Tag.Technologies)})"
                : $"NFC {e.Tag.Uid}: {content}";
        });
    }

    private void OnNfcError(object? sender, NfcErrorEventArgs e)
    {
        MainThread.BeginInvokeOnMainThread(() => LastNfcLabel.Text = e.Message);
    }

    private void OnRfidInventoryClicked(object? sender, EventArgs e)
    {
        if (_inventoryRunning)
        {
            _rfid.StopInventory();
        }
        else
        {
            _inventoryEpcs.Clear();
            _rfid.StartInventory();
        }

        SetInventoryRunning(!_inventoryRunning);
    }

    private void SetInventoryRunning(bool running)
    {
        _inventoryRunning = running;
        RfidInventoryButton.Text = running ? "Parar inventário RFID" : "Iniciar inventário RFID";
    }

    // Disparado na main thread (RfidTagBuffer nativo).
    private void OnRfidTagsRead(object? sender, RfidTagsEventArgs e)
    {
        foreach (var tag in e.Tags)
        {
            _inventoryEpcs.Add(tag.Epc);
        }

        _lastEpc = e.Tags[^1].Epc;
        RfidLabel.Text = $"RFID: {_inventoryEpcs.Count} tags (última: {_lastEpc})";
    }

    // Operacao por tag (so com o SDK Zebra RFID API3): le as 6 primeiras
    // palavras do banco TID, o identificador gravado de fabrica no chip.
    private async void OnReadTidClicked(object? sender, EventArgs e)
    {
        if (!_rfid.SupportsTagOperations)
        {
            RfidLabel.Text = "Operações por tag exigem o SDK Zebra RFID API3.";
            return;
        }

        if (_lastEpc is null)
        {
            RfidLabel.Text = "Faça um inventário antes.";
            return;
        }

        if (_inventoryRunning)
        {
            _rfid.StopInventory();
            SetInventoryRunning(false);
        }

        try
        {
            var tid = await _rfid.ReadMemoryAsync(_lastEpc, RfidMemoryBank.Tid, wordOffset: 0, wordCount: 6);
            RfidLabel.Text = $"TID de {_lastEpc}: {tid}";
        }
        catch (InvalidOperationException ex)
        {
            RfidLabel.Text = ex.Message;
        }
    }

    private void OnRfidError(object? sender, RfidErrorEventArgs e) => RfidLabel.Text = e.Message;

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

    // Digitalizacao pela tela do Google Play Services: ate 5 paginas, com PDF.
    private async void OnDocumentScannerClicked(object? sender, EventArgs e)
    {
        try
        {
            var result = await _documentScanner.ScanAsync(pageLimit: 5);
            LastScanLabel.Text = result is null
                ? "Digitalização cancelada."
                : $"Documento: {result.PageCount} página(s) - {result.PdfUri}";
        }
        catch (InvalidOperationException ex)
        {
            HandleScanError(ex.Message);
        }
    }

    private async void OnCodeScannerClicked(object? sender, EventArgs e)
    {
        try
        {
            var result = await _codeScanner.ScanAsync();
            if (result is null)
            {
                LastScanLabel.Text = "Leitura cancelada.";
                return;
            }

            HandleScanResult(result);
        }
        catch (InvalidOperationException ex)
        {
            HandleScanError(ex.Message);
        }
    }

    // Motores do pacote principal + do ScannerVcIaRfid.Maui.VisionExtras.
    private static readonly (string Id, string Name)[] Engines =
    [
        (VisionEngines.Barcode, "Código de barras"),
        (VisionEngines.Ocr, "OCR"),
        (VisionExtraEngines.Objects, "Objetos"),
        (VisionExtraEngines.Labels, "Rótulos"),
        (VisionExtraEngines.Faces, "Rostos")
    ];

    private int _engineIndex;

    private void OnSwitchEngineClicked(object? sender, EventArgs e)
    {
        // Engine e uma BindableProperty: o handler troca o motor nativo na
        // hora, mesmo com a camera ligada.
        _engineIndex = (_engineIndex + 1) % Engines.Length;
        CameraScannerView.Engine = Engines[_engineIndex].Id;
        SwitchEngineButton.Text = $"Motor: {Engines[_engineIndex].Name}";
    }
}
