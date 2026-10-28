# RfidReaderFactory (modulo rfid) cria o ZebraApi3RfidReader por reflection,
# para nao depender deste modulo. Sem esta regra, o R8 do app remove a classe.
-keep class com.scannervciarfid.rfid.zebra.ZebraApi3RfidReader { <init>(); }
# O SDK da Zebra usa reflection internamente.
-keep class com.zebra.rfid.api3.** { *; }
-dontwarn com.zebra.rfid.api3.**
