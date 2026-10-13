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
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
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
    //  EXPORT EXCEL (.xlsx)
    // ============================================
    fun exportToExcel(
        context: Context,
        transactions: List<Transaction>,
        startDate: String,
        endDate: String
    ): File? {
        return try {
            val wb: Workbook = XSSFWorkbook()
            val sheet = wb.createSheet("Laporan Penjualan")

            var rowNum = 0

            // Header toko
            var row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue(AppConfig.NAMA_TOKO)
            row.createCell(4).setCellValue("Laporan: $startDate s/d $endDate")

            rowNum++ // spasi

            // Judul kolom
            row = sheet.createRow(rowNum++)
            val headers = listOf(
                "No", "Invoice", "Tanggal", "Kasir",
                "Metode", "Total", "Bayar", "Kembali"
            )
            headers.forEachIndexed { i, h ->
                row.createCell(i).setCellValue(h)
            }

            // Data transaksi
            val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            var totalOmzet = 0.0
            var totalTunai = 0.0
            var totalQris = 0.0

            transactions.forEachIndexed { idx, t ->
                val r = sheet.createRow(rowNum++)
                r.createCell(0).setCellValue((idx + 1).toDouble())
                r.createCell(1).setCellValue(t.invoiceNo)
                r.createCell(2).setCellValue(dateFmt.format(Date(t.timestamp)))
                r.createCell(3).setCellValue(t.cashierName)
                r.createCell(4).setCellValue(t.paymentMethod)
                r.createCell(5).setCellValue(t.total)
                r.createCell(6).setCellValue(t.amountPaid)
                r.createCell(7).setCellValue(t.change)

                totalOmzet += t.total
                if (t.paymentMethod == "CASH") totalTunai += t.total
                else totalQris += t.total
            }

            // Ringkasan
            rowNum++
            row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue("RINGKASAN")
            row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue("Total Transaksi")
            row.createCell(1).setCellValue(transactions.size.toDouble())
            row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue("Total Omzet")
            row.createCell(1).setCellValue(totalOmzet)
            row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue("Tunai")
            row.createCell(1).setCellValue(totalTunai)
            row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue("QRIS")
            row.createCell(1).setCellValue(totalQris)

            // Auto-size kolom
            for (i in headers.indices) {
                sheet.autoSizeColumn(i)
            }

            // Simpan
            val fileName = "Laporan_${AppConfig.NAMA_TOKO.replace(" ", "_")}_${System.currentTimeMillis()}.xlsx"
            val file = File(getExportDir(context), fileName)
            val fos = FileOutputStream(file)
            wb.write(fos)
            fos.close()
            wb.close()

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
            val fileName = "Laporan_${AppConfig.NAMA_TOKO.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val file = File(getExportDir(context), fileName)

            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val doc = Document(pdf)

            // Header toko
            doc.add(
                Paragraph(AppConfig.NAMA_TOKO)
                    .setBold()
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            doc.add(
                Paragraph(AppConfig.TAGLINE)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            doc.add(
                Paragraph("${AppConfig.ALAMAT} | Telp: ${AppConfig.TELEPON}")
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            doc.add(Paragraph("\n"))

            doc.add(
                Paragraph("LAPORAN PENJUALAN")
                    .setBold()
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            doc.add(
                Paragraph("Periode: $startDate s/d $endDate")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            doc.add(Paragraph("\n"))

            // Tabel
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

            // Ringkasan
            doc.add(Paragraph("\n"))
            doc.add(Paragraph("RINGKASAN").setBold().setFontSize(12))
            doc.add(Paragraph("Total Transaksi : ${transactions.size}"))
            doc.add(Paragraph("Total Omzet : ${formatRp(totalOmzet)}"))
            doc.add(Paragraph("Total Tunai : ${formatRp(totalTunai)}"))
            doc.add(Paragraph("Total QRIS : ${formatRp(totalQris)}"))

            doc.add(Paragraph("\n\n"))
            doc.add(
                Paragraph(
                    "Dicetak pada: " +
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                ).setFontSize(8)
            )

            doc.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ============================================
    //  SHARE FILE (ke WA, Email, dll)
    // ============================================
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
                    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
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
