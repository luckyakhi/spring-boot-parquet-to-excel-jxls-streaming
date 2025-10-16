package com.example.jxlsstream.parquet;

import com.example.jxlsstream.dto.TransactionRecord;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.NoSuchElementException;

@Service
public class ParquetService {

  public Iterator<TransactionRecord> readAsIterator(String parquetFile) throws IOException {
    if (!Files.exists(Paths.get(parquetFile))) {
      throw new IOException("Parquet file not found: " + parquetFile);
    }
    var conf = HadoopConfigurationFactory.create();
    ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(new org.apache.hadoop.fs.Path(parquetFile))
        .withConf(conf)
        .build();

    return new Iterator<>() {
      GenericRecord nextRecord;
      boolean fetched = false;

      private void fetch() {
        if (fetched) return;
        try {
          nextRecord = reader.read();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        fetched = true;
      }

      @Override public boolean hasNext() {
        fetch();
        if (nextRecord == null) {
          try { reader.close(); } catch (IOException ignored) {}
        }
        return nextRecord != null;
      }

      @Override public TransactionRecord next() {
        if (!hasNext()) throw new NoSuchElementException();
        GenericRecord rec = nextRecord;
        fetched = false;
        nextRecord = null;
        return map(rec);
      }

      private TransactionRecord map(GenericRecord r) {
        String accountId = asString(r.get("accountId"));
        String currency = asString(r.get("currency"));
        double amount = r.get("amount") == null ? 0.0 : ((Number) r.get("amount")).doubleValue();
        String d = asString(r.get("txnDate"));
        return new TransactionRecord(accountId, currency, amount, java.time.LocalDate.parse(d));
      }

      private String asString(Object o) {
        if (o == null) return null;
        if (o instanceof CharSequence cs) return cs.toString();
        if (o instanceof GenericData.EnumSymbol es) return es.toString();
        return o.toString();
      }
    };
  }
}
