using System.Windows.Input;
using Microsoft.Maui.Controls;
using ScannerVcIaRfid.Maui.Models;

namespace ScannerVcIaRfid.Maui.Controls;

/// <summary>
/// Controle cross-platform que renderiza a VisionScannerView nativa via Handler.
/// A camada de plataforma (Platforms/Android/Handlers/VisionScannerHandler) chama
/// RaiseScanned/RaiseScanError para repassar eventos do leitor nativo para o XAML.
/// </summary>
public class VisionScanner : View
{
    public static readonly BindableProperty EngineProperty = BindableProperty.Create(
        nameof(Engine),
        typeof(string),
        typeof(VisionScanner),
        VisionEngines.Barcode);

    /// <summary>
    /// Motor de analise de frame (ver <see cref="VisionEngines"/>). Pode mudar
    /// com a camera ligada: o motor novo passa a analisar os proximos frames.
    /// </summary>
    public string Engine
    {
        get => (string)GetValue(EngineProperty);
        set => SetValue(EngineProperty, value);
    }

    public static readonly BindableProperty ScanCommandProperty = BindableProperty.Create(
        nameof(ScanCommand),
        typeof(ICommand),
        typeof(VisionScanner));

    public ICommand? ScanCommand
    {
        get => (ICommand?)GetValue(ScanCommandProperty);
        set => SetValue(ScanCommandProperty, value);
    }

    public event EventHandler<ScanResultEventArgs>? Scanned;
    public event EventHandler<ScanErrorEventArgs>? ScanError;

    public void RaiseScanned(ScanResult result)
    {
        Scanned?.Invoke(this, new ScanResultEventArgs(result));

        if (ScanCommand?.CanExecute(result) == true)
        {
            ScanCommand.Execute(result);
        }
    }

    public void RaiseScanError(string message)
    {
        ScanError?.Invoke(this, new ScanErrorEventArgs(message));
    }

    /// <summary>
    /// Engatilha o inicio da captura via camera no lado nativo. Nao inicia
    /// automaticamente ao conectar o Handler (strict gating) - o consumidor
    /// (code-behind/ViewModel) deve chamar isto explicitamente, tipicamente
    /// apos confirmar a permissao de camera em runtime.
    /// </summary>
    public void StartCamera()
    {
        Handler?.Invoke(nameof(StartCamera));
    }
}
