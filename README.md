# Parquet → Excel (XLSX) with **JXLS + SXSSF Streaming** (Spring Boot)

This sample demonstrates a **template-driven** Excel export that scales to **200k+ rows** by
combining **JXLS** with **Apache POI SXSSF** (streaming).

## Highlights
- **Template-driven** formatting: headers, fonts, column widths are defined in the template.
- **Streaming writer**: uses `PoiTransformer.createSxssfTransformer(..., windowSize, compressTmp)`
  so only a small number of rows are kept in memory.
- **Lazy Parquet read**: the Parquet reader exposes an `Iterator<TransactionRecord>` to avoid
  materializing the entire dataset in heap.
- **Integration test** generates Parquet and produces XLSX.

## Run tests
```bash
mvn -q -DskipTests=false test
```

Increase the row count in the test to `200_000` on a machine with adequate disk space for
temporary SXSSF files to validate large-scale behavior.

## Template
A minimal **JXLS** template is created at runtime by `TemplateBuilder`:
- Row 0: column headers
- Row 1: cells contain `${r.accountId}`, `${r.currency}`, `${r.amount}`, `${r.txnDate}`
- A comment on cell A2 contains: `jx:each(items="rows" var="r" lastCell="D2")`

> In production, store a versioned template under `src/main/resources/templates/your_template.xlsx`
  and load it via `ClassPathResource`. You can add footers, print areas, protection, named ranges,
  and other regulatory niceties in the template itself.
