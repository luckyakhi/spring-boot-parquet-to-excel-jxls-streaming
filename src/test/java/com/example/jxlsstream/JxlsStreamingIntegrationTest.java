package com.example.jxlsstream;

import com.example.jxlsstream.dto.TransactionRecord;
import com.example.jxlsstream.excel.JxlsStreamingExporter;
import com.example.jxlsstream.parquet.ParquetService;
import com.example.jxlsstream.template.TemplateBuilder;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

@SpringBootTest
class JxlsStreamingIntegrationTest {

  @TempDir Path tmp;

  @Autowired ParquetService parquetService;
  @Autowired JxlsStreamingExporter exporter;

  @Test
  void jxls_streaming_over_50k_rows_succeeds() throws Exception {
    Path parquet = tmp.resolve("transactions.parquet");
    Path xlsx = tmp.resolve("transactions.xlsx");

    // 1) Create a Parquet with ~50k rows (increase to 200k locally if needed)
    writeSampleParquet(parquet.toString(), 50_000);

    // 2) Lazily read rows
    Iterator<TransactionRecord> it = parquetService.readAsIterator(parquet.toString());

    // 3) Build an in-memory JXLS template (could also store a static file under resources)
    ByteArrayOutputStream tmplOut = new ByteArrayOutputStream();
    TemplateBuilder.createBasicTemplate(tmplOut);
    byte[] tmplBytes = tmplOut.toByteArray();

    // 4) Render with JXLS + SXSSF streaming
    try (OutputStream out = Files.newOutputStream(xlsx)) {
      exporter.render(new ByteArrayInputStream(tmplBytes), out, it, 1000);
    }

    // 5) Assertions
    Assertions.assertTrue(Files.exists(xlsx));
    long size = Files.size(xlsx);
    Assertions.assertTrue(size > 0, "XLSX appears to be empty");

    try (InputStream in = Files.newInputStream(xlsx); Workbook workbook = WorkbookFactory.create(in)) {
      Sheet sheet = workbook.getSheet("Transactions");
      Assertions.assertNotNull(sheet, "Expected Transactions sheet");
      Assertions.assertEquals("AccountId", sheet.getRow(0).getCell(0).getStringCellValue());
      int physicalRows = sheet.getPhysicalNumberOfRows();
      Assertions.assertTrue(physicalRows >= 50_001,
          "Expected at least 50k data rows, but found only " + (physicalRows - 1));
    }
  }

  private void writeSampleParquet(String file, int rows) throws Exception {
    String schemaJson = """
        {
          "type": "record",
          "name": "Txn",
          "fields": [
            {"name": "accountId", "type": "string"},
            {"name": "currency", "type": "string"},
            {"name": "amount", "type": "double"},
            {"name": "txnDate", "type": "string"}
          ]
        }
        """;
    Schema schema = new Schema.Parser().parse(schemaJson);
    try (ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(new org.apache.hadoop.fs.Path(file))
        .withSchema(schema)
        .withCompressionCodec(CompressionCodecName.SNAPPY)
        .withPageSize(128 * 1024)
        .withRowGroupSize(128L * 1024 * 1024)
        .build()) {
      java.time.LocalDate base = java.time.LocalDate.of(2025, 1, 1);
      for (int i = 0; i < rows; i++) {
        GenericRecord rec = new GenericData.Record(schema);
        rec.put("accountId", String.format("ACC-%06d", i));
        rec.put("currency", (i % 2 == 0) ? "INR" : "USD");
        rec.put("amount", i * 1.23);
        rec.put("txnDate", base.plusDays(i % 365).toString());
        writer.write(rec);
      }
    }
  }
}
