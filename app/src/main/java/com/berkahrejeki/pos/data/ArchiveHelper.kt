package com.berkahrejeki.pos.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ArchiveHelper {

    // Batas umur transaksi aktif (dalam hari). 90 hari = 3 bulan.
    private const val ARCHIVE_DAYS = 90L
    private const val ARCHIVE_FOLDER = "arsip_transaksi"

    /**
     * Cek & arsipkan transaksi lama (> 90 hari) ke CSV
     * Return: jumlah transaksi yang diarsipkan
     */
    suspend fun autoArchive(
        context: Context,
        trxDao: TransactionDao
    ): Int {
        val cutoff = System.currentTimeMillis() - (ARCHIVE_DAYS * 24L * 60L * 60L * 1000L)
        val oldTrx = trxDao.getByRangeOnce(0L, cutoff)

        if (oldTrx.isEmpty()) return 0

        val saved = saveToCsv(context, oldTrx)
        if (!saved) return 0

        // Hapus dari DB setelah tersimpan
        oldTrx.forEach { trx ->
            trxDao.deleteById(trx.id)
        }

        return oldTrx.size
    }

    /**
     * Simpan list transaksi ke file CSV di folder app
     */
    private fun saveToCsv(context: Context, transactions: List<Transaction>): Boolean {
        return try {
            val dir = File(context.getExternalFilesDir(null), ARCHIVE_FOLDER)
            if (!dir.exists()) dir.mkdirs()

            val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fileNameFmt = SimpleDateFormat("yyyyMM", Locale.getDefault())

            // Group per bulan (arsip_202610.csv, arsip_202609.csv, dst)
            val grouped = transactions.groupBy {
                fileNameFmt.format(Date(it.timestamp))
            }

            grouped.forEach { (bulan, list) ->
                val file = File(dir, "arsip_$bulan.csv")
                val sb = StringBuilder()

                // Header (hanya kalau file baru)
                if (!file.exists() || file.length() == 0L) {
                    sb.append("No,Invoice,Tanggal,Kasir,Metode,Total,Bayar,Kembali,Items\n")
                }

                // Hitung offset nomor urut kalau file sudah ada
                val existingLines = if (file.exists()) {
                    try { file.readLines().size - 1 } catch (_: Exception) { 0 }
                } else 0

                list.forEachIndexed { idx, t ->
                    sb.append("${existingLines + idx + 1},")
                    sb.append("\"${t.invoiceNo}\",")
                    sb.append("\"${dateFmt.format(Date(t.timestamp))}\",")
                    sb.append("\"${t.cashierName}\",")
                    sb.append("${t.paymentMethod},")
                    sb.append("${t.total},")
                    sb.append("${t.amountPaid},")
                    sb.append("${t.change},")
                    // JSON di-escape tanda kutip
                    sb.append("\"${t.itemsJson.replace("\"", "\"\"")}\"\n")
                }

                file.appendText(sb.toString())
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Ukuran database saat ini (MB)
     */
    fun getDatabaseSizeMb(context: Context): Double {
        return try {
            val db = context.getDatabasePath("berkah_rejeki.db")
            if (db.exists()) db.length() / (1024.0 * 1024.0) else 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Bersihkan file arsip lama (> 6 bulan)
     */
    fun cleanOldArchives(context: Context, monthsOld: Int = 6) {
        try {
            val dir = File(context.getExternalFilesDir(null), ARCHIVE_FOLDER)
            if (!dir.exists()) return

            val cutoff = System.currentTimeMillis() - (monthsOld * 30L * 24L * 60L * 60L * 1000L)
            val fmt = SimpleDateFormat("yyyyMM", Locale.getDefault())

            dir.listFiles()?.forEach { file ->
                if (file.name.startsWith("arsip_")) {
                    val dateStr = file.name.replace("arsip_", "").replace(".csv", "")
                    try {
                        val fileDate = fmt.parse(dateStr)
                        if (fileDate != null && fileDate.time < cutoff) file.delete()
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Daftar file arsip yang ada
     */
    fun listArchives(context: Context): List<File> {
        val dir = File(context.getExternalFilesDir(null), ARCHIVE_FOLDER)
        return dir.listFiles()?.filter { it.name.endsWith(".csv") }?.sortedBy { it.name } ?: emptyList()
    }

    /**
     * Total ukuran semua file arsip (MB)
     */
    fun getArchiveSizeMb(context: Context): Double {
        return try {
            val dir = File(context.getExternalFilesDir(null), ARCHIVE_FOLDER)
            val total = dir.listFiles()?.sumOf { it.length() } ?: 0L
            total / (1024.0 * 1024.0)
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Folder arsip untuk di-share
     */
    fun getArchiveDir(context: Context): File =
        File(context.getExternalFilesDir(null), ARCHIVE_FOLDER)
}
