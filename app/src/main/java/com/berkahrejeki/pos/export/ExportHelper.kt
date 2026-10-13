package com.berkahrejeki.pos.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.berkahrejeki.pos.AppConfig
import com.berkahrejeki.pos.data.Transaction
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {

    private fun formatRp(v: Double): String =
        "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(v)

    private fun getExportDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ============================================
    //  EXPORT CSV (bisa dibuka di Excel)
    // ============================================
    fun exportToCsv(
        context: Context,
        transactions: List<Transaction>,
        startDate: String,
        endDate: String
    ): File? {
        return try {
            val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val sb = StringBuilder()

            sb.append("${AppConfig.NAMA_TOKO}\n")
            sb.append("${AppConfig.TAGLINE}\n")
            sb.append("${AppConfig.ALAMAT}\n")
            sb.append("Telp: ${AppConfig.TELEPON}\n\n")
            sb.append("Laporan: $startDate s/d $endDate\n\n")
            sb.append("No,Invoice,Tanggal,Kasir,Metode,Total,Bayar,Kembali\n")

            var totalOmzet = 0.0
            var totalTunai = 0.0
            var totalQris = 0.0

            transactions.forEachIndexed { idx, t ->
                sb.append("${idx + 1},")
                sb.append("${t.invoiceNo},")
                sb.append("${dateFmt.format(Date(t.timestamp))},")
                sb.append("${t.cashierName},")
                sb.append("${t.paymentMethod},")
                sb.append("${t.total},")
                sb.append("${t.amountPaid},")
                sb.append("${t.change}\n")

                totalOmzet += t.total
                if (t.paymentMethod == "CASH") totalTunai += t.total
                else totalQris += t.total
            }

            sb.append("\n")
            sb.append("RINGKASAN\n")
            sb.append("Total Transaksi,${transactions.size}\n")
            sb.append("Total Omzet,${totalOmzet}\n")
            sb.append("Tunai,${totalTunai}\n")
            sb.append("QRIS,${totalQris}\n")

            val fileName =
                "Laporan_${AppConfig.NAMA_TOKO.replace(" ", "_")}_${System.currentTimeMillis()}.csv"
            val file = File(getExportDir(context), fileName)
            file.writeText(sb.toString())

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ============================================
    //  EXPORT PDF
    // ============================================
    fun exportToPdf(
        context: Context,
        transactions: List<Transaction>,
        startDate: String,
        endDate: String
    ): File? {
        return try {
            val fileName =
                "Laporan_${AppConfig.NAMA_TOKO.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val file = File(getExportDir(context), fileName)

            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val doc = Document(pdf)

            doc.add(
                Paragraph(AppConfig.NAMA_TOKO)
                    .setBold()
                    .setFontSize(18f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            doc.add(
                Paragraph(AppConfig.TAGLINE)
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            doc.add(
                Paragraph("${AppConfig.ALAMAT} | Telp: ${AppConfig.TELEPON}")
                    .setFontSize(9f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            doc.add(Paragraph("\n"))
            doc.add(
                Paragraph("LAPORAN PENJUALAN")
                    .setBold()
                    .setFontSize(14f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            doc.add(
                Paragraph("Periode: $startDate s/d $endDate")
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            doc.add(Paragraph("\n"))

            val table = Table(floatArrayOf(30f, 90f, 90f, 70f, 60f, 80f))
            listOf("No", "Invoice", "Tanggal", "Kasir", "Metode", "Total")
                .forEach { table.addHeaderCell(it) }

            val dateFmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            var totalOmzet = 0.0
            var totalTunai = 0.0
            var totalQris = 0.0

            transactions.forEachIndexed { idx, t ->
                table.addCell("${idx + 1}")
                table.addCell(t.invoiceNo)
                table.addCell(dateFmt.format(Date(t.timestamp)))
                table.addCell(t.cashierName)
                table.addCell(t.paymentMethod)
                table.addCell(formatRp(t.total))

                totalOmzet += t.total
                if (t.paymentMethod == "CASH") totalTunai += t.total
                else totalQris += t.total
            }
            doc.add(table)

            doc.add(Paragraph("\n"))
            doc.add(Paragraph("RINGKASAN").setBold().setFontSize(12f))
            doc.add(Paragraph("Total Transaksi : ${transactions.size}"))
            doc.add(Paragraph("Total Omzet : ${formatRp(totalOmzet)}"))
            doc.add(Paragraph("Total Tunai : ${formatRp(totalTunai)}"))
            doc.add(Paragraph("Total QRIS : ${formatRp(totalQris)}"))

            doc.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareFile(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when (file.extension) {
                    "pdf" -> "application/pdf"
                    "csv" -> "text/csv"
                    else -> "*/*"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan ${AppConfig.NAMA_TOKO}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Laporan"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
