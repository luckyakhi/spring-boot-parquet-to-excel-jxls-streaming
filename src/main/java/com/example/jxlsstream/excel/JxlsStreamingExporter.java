package com.example.jxlsstream.excel;

import com.example.jxlsstream.dto.TransactionRecord;
import org.jxls.common.Context;
import org.jxls.util.JxlsHelper;
import org.jxls.transform.poi.PoiTransformer;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;

@Component
@DependsOn("poiTempFileConfigurer")
public class JxlsStreamingExporter {

  public void render(InputStream templateXlsx,
                     OutputStream out,
                     Iterator<TransactionRecord> rows,
                     int windowSize) throws Exception {

    try (Workbook templateWorkbook = new XSSFWorkbook(templateXlsx)) {
      OutputStream safeOut = new NonClosingOutputStream(out);
      // Using compressed temp files (`compressTmpFiles=true`) causes SXSSF to write the
      // worksheet data into GZIP archives under the `poi-sxssf-sheet-xml*.gz` naming
      // pattern.  On Windows these archived temp files sometimes disappear while the
      // workbook is still being written, leading to `FileNotFoundException` errors such as
      // `The system cannot find the file specified`.  To avoid that race we keep the
      // defaults (no compression) which stores the sheet data as plain XML files.
      //
      // The temp files are slightly larger on disk, but they are short lived and the
      // export succeeds reliably across operating systems.
      PoiTransformer transformer = PoiTransformer.createSxssfTransformer(templateWorkbook, safeOut, windowSize, false);

      Context ctx = PoiTransformer.createInitialContext();
      Iterable<TransactionRecord> iterableRows = new Iterable<>() {
        boolean consumed = false;

        @Override
        public Iterator<TransactionRecord> iterator() {
          if (consumed) {
            return Collections.emptyIterator();
          }
          consumed = true;
          return rows;
        }
      };
      ctx.putVar("rows", iterableRows);

      JxlsHelper jh = JxlsHelper.getInstance()
          .setUseFastFormulaProcessor(true);

      jh.processTemplate(ctx, transformer);

      Workbook wb = transformer.getWorkbook();
      wb.setForceFormulaRecalculation(true);
      transformer.write();
      safeOut.flush();
    }
  }

  private static class NonClosingOutputStream extends FilterOutputStream {
    NonClosingOutputStream(OutputStream delegate) {
      super(delegate);
    }

    @Override
    public void close() throws IOException {
      flush();
    }
  }
}
