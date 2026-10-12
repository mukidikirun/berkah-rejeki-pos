package com.berkahrejeki.pos.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.berkahrejeki.pos.data.Transaction
import org.json.JSONArray
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class ThermalPrinter(private val context: Context) {
    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        return try {
            val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            adapter.cancelDiscovery()
            socket = device.createRfcommSocketToServiceRecord(
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            )
            socket?.connect()
            output = socket?.outputStream
            true
        } catch (e: Exception) {
            e.printStackTrace(); false
        }
    }

    fun disconnect() {
        try { output?.close(); socket?.close() } catch (_: Exception) {}
    }

    private fun write(bytes: ByteArray) {
        output?.write(bytes)
        output?.flush()
    }

    private fun text(s: String) = write(s.toByteArray(Charsets.UTF_8))
    private fun newline() = text("\n")

    fun printReceipt(trx: Transaction) {
        val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val items = JSONArray(trx.itemsJson)

        write(byteArrayOf(0x1B, 0x40))
        write(byteArrayOf(0x1B, 0x61, 0x01))
        write(byteArrayOf(0x1B, 0x21, 0x10))
        text("BERKAH REJEKI"); newline()
        write(byteArrayOf(0x1B, 0x21, 0x00))
        text("Toko Retail & Sembako"); newline()
        text("DS.Ringin kembar RT. 02 Rw.07"); newline()
        text("Telp: 085843866617); newline()
        text("---Terimakasih belanja di toko kami---------"); newline()

        write(byteArrayOf(0x1B, 0x61, 0x00))
        text("No : ${trx.invoiceNo}"); newline()
        text("Tgl: ${dateFmt.format(Date(trx.timestamp))}"); newline()
        text("Kasir: ${trx.cashierName}"); newline()
        text("--------------------------------"); newline()

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            text(item.getString("name")); newline()
            text("  ${item.getInt("qty")} x ${formatRp(item.getDouble("price"))} = ${formatRp(item.getDouble("subtotal"))}"); newline()
        }

        text("--------------------------------"); newline()
        text("TOTAL : ${formatRp(trx.total)}"); newline()
        text("BAYAR (${trx.paymentMethod}) : ${formatRp(trx.amountPaid)}"); newline()
        if (trx.paymentMethod == "CASH") text("KEMBALI : ${formatRp(trx.change)}"); newline()
        text("================================"); newline()

        write(byteArrayOf(0x1B, 0x61, 0x01))
        text("Terima kasih telah berbelanja"); newline()
        text("Di Toko BERKAH REJEKI"); newline()
        text("Semoga Rezeki Anda Berkah"); newline()
        newline(); newline(); newline()
        write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
    }

    private fun formatRp(v: Double) = "Rp " + String.format("%,.0f", v).replace(',', '.')
}
