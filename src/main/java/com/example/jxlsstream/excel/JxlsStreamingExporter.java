package com.example.jxlsstream.excel;

import com.example.jxlsstream.dto.TransactionRecord;
import org.jxls.common.Context;
import org.jxls.transform.Transformer;
import org.jxls.util.JxlsHelper;
import org.jxls.transform.poi.PoiTransformer;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

@Component
public class JxlsStreamingExporter {

  public void render(InputStream templateXlsx,
                     OutputStream out,
                     Iterator<TransactionRecord> rows,
                     int windowSize) throws Exception {

    Transformer transformer = PoiTransformer.createSxssfTransformer(templateXlsx, out, windowSize, true);

    Context ctx = new Context();
    ctx.putVar("rows", rows);

    JxlsHelper jh = JxlsHelper.getInstance()
        .setUseFastFormulaProcessor(true);

    jh.processTemplate(ctx, transformer);

    Workbook wb = ((PoiTransformer) transformer).getWorkbook();
    wb.setForceFormulaRecalculation(true);
  }
}
