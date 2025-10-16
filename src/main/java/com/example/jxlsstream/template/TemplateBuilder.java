package com.example.jxlsstream.template;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;

public class TemplateBuilder {

  public static void createBasicTemplate(OutputStream out) throws IOException {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      XSSFSheet sh = wb.createSheet("Transactions");
      int[] widths = {20, 10, 15, 12};
      for (int c = 0; c < widths.length; c++) sh.setColumnWidth(c, widths[c] * 256);

      CellStyle header = wb.createCellStyle();
      Font hFont = wb.createFont(); hFont.setBold(true); header.setFont(hFont);

      Row r0 = sh.createRow(0);
      String[] hdrs = {"AccountId", "Currency", "Amount", "TxnDate"};
      for (int c = 0; c < hdrs.length; c++) {
        Cell cell = r0.createCell(c);
        cell.setCellValue(hdrs[c]);
        cell.setCellStyle(header);
      }

      Row r1 = sh.createRow(1);
      r1.createCell(0).setCellValue("${r.accountId}");
      r1.createCell(1).setCellValue("${r.currency}");
      r1.createCell(2).setCellValue("${r.amount}");
      r1.createCell(3).setCellValue("${r.txnDate}");

      XSSFCreationHelper helper = wb.getCreationHelper();
      Drawing<?> drawing = sh.createDrawingPatriarch();
      ClientAnchor anchor = new XSSFClientAnchor();
      anchor.setCol1(0); anchor.setCol2(2);
      anchor.setRow1(1); anchor.setRow2(3);
      Comment comment = drawing.createCellComment(anchor);
      comment.setString(helper.createRichTextString("jx:each(items=\"rows\" var=\"r\" lastCell=\"D2\")"));
      r1.getCell(0).setCellComment(comment);

      sh.createFreezePane(0,1);

      wb.write(out);
    }
  }
}
