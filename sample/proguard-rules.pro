# App de exemplo, sem minificacao habilitada (isMinifyEnabled = false).
# Arquivo existe apenas porque buildTypes.release referencia "proguard-rules.pro"
# em proguardFiles(...) - o AGP falha o build se o arquivo nao existir,
# mesmo com a minificacao desligada (ja vimos esse erro antes no modulo raiz).
