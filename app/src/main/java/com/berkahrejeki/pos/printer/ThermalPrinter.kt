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
            e.printStackTrace()
            false
        }
    }

    fun disconnect() {
        try {
            output?.close()
            socket?.close()
        } catch (_: Exception) {
        }
    }

    private fun write(bytes: ByteArray) {
        output?.write(bytes)
        output?.flush()
    }

    private fun text(s: String) {
        write(s.toByteArray(Charsets.UTF_8))
    }

    private fun newline() {
        text("\n")
    }

    fun printReceipt(trx: Transaction) {
        val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val items = JSONArray(trx.itemsJson)

        // Init printer
        write(byteArrayOf(0x1B, 0x40))
        // Center align
        write(byteArrayOf(0x1B, 0x61, 0x01))
        // Double height
        write(byteArrayOf(0x1B, 0x21, 0x10))
        text("BERKAH REJEKI")
        newline()
        write(byteArrayOf(0x1B, 0x21, 0x00))
        text("Toko Retail & Sembako")
        newline()
        text("Dsn. Ringin Kembar")
        newline()
        text("Telp: +62 858-4386-6617")
        newline()
        text("--------------------------------")
        newline()

        // Left align
        write(byteArrayOf(0x1B, 0x61, 0x00))
        text("No : " + trx.invoiceNo)
        newline()
        text("Tgl: " + dateFmt.format(Date(trx.timestamp)))
        newline()
        text("Kasir: " + trx.cashierName)
        newline()
        text("--------------------------------")
        newline()

        // Items
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val name = item.getString("name")
            val qty = item.getInt("qty")
            val price = item.getDouble("price")
            val sub = item.getDouble("subtotal")
            text(name)
            newline()
            text("  " + qty + " x " + formatRp(price) + " = " + formatRp(sub))
            newline()
        }

        text("--------------------------------")
        newline()
        text("TOTAL : " + formatRp(trx.total))
        newline()
        text("BAYAR (" + trx.paymentMethod + ") : " + formatRp(trx.amountPaid))
        newline()
        if (trx.paymentMethod == "CASH") {
            text("KEMBALI : " + formatRp(trx.change))
            newline()
        }
        text("================================")
        newline()

        // Center align footer
        write(byteArrayOf(0x1B, 0x61, 0x01))
        text("Terima kasih telah berbelanja")
        newline()
        text("Di Toko BERKAH REJEKI")
        newline()
        text("Semoga Rezeki Anda Berkah")
        newline()
        newline()
        newline()
        newline()
        // Cut paper
        write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
    }

    private fun formatRp(v: Double): String {
        return "Rp " + String.format("%,.0f", v).replace(',', '.')
    }
}
