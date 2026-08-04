package com.aiexportagent.scraping.upload;

import com.aiexportagent.common.exception.ApiException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns uploaded bytes into a header row plus data rows. Knows nothing about
 * suppliers — just cells.
 *
 * <p>Every method here handles a file supplied by someone we do not trust.
 */
public final class SpreadsheetParser {

    private SpreadsheetParser() {
    }

    /**
     * POI's zip-bomb guards. An {@code .xlsx} is a zip archive, so a few-KB
     * upload can expand to gigabytes and take the container down with it —
     * <strong>a file-size cap does not protect against this</strong>, which is
     * what makes it an easy mistake.
     *
     * <p>These settings are static and global to POI, so they are applied once
     * in a static initialiser rather than per request.
     */
    static {
        ZipSecureFile.setMinInflateRatio(0.01d);
        ZipSecureFile.setMaxEntrySize(200L * 1024 * 1024);
    }

    /** Header row plus data rows, all cells as raw strings. */
    public record Sheet2D(List<String> headers, List<List<String>> rows) {
    }

    public static Sheet2D parse(String fileName, byte[] bytes, int maxRows) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) {
            return parseCsv(bytes, maxRows);
        }
        if (lower.endsWith(".xlsx")) {
            return parseXlsx(bytes, maxRows);
        }
        // .xlsm is rejected here: macros are never executed by this reader, but
        // there is no reason to accept a macro-enabled workbook at all.
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "Unsupported file type. Upload a .xlsx or .csv file.");
    }

    private static Sheet2D parseCsv(byte[] bytes, int maxRows) {
        String text = CharsetDetector.decode(bytes);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreEmptyLines(true)
                .setAllowMissingColumnNames(true)
                .build();

        try (CSVParser parser = format.parse(new StringReader(text))) {
            List<String> headers = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();

            for (CSVRecord record : parser) {
                List<String> values = new ArrayList<>(record.size());
                for (int i = 0; i < record.size(); i++) {
                    values.add(record.get(i));
                }
                if (headers.isEmpty()) {
                    headers.addAll(values);
                    continue;
                }
                if (rows.size() >= maxRows) break;
                rows.add(values);
            }
            return new Sheet2D(headers, rows);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Could not read this CSV file: " + e.getMessage());
        }
    }

    private static Sheet2D parseXlsx(byte[] bytes, int maxRows) {
        // DataFormatter renders the CACHED value of a formula cell. A
        // FormulaEvaluator is deliberately never constructed — evaluating
        // formulas is running computation supplied by an untrusted file.
        DataFormatter formatter = new DataFormatter();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "This workbook has no sheets.");
            }
            Sheet sheet = workbook.getSheetAt(0);

            List<String> headers = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();

            for (Row row : sheet) {
                List<String> values = new ArrayList<>();
                short last = row.getLastCellNum();
                for (int i = 0; i < last; i++) {
                    Cell cell = row.getCell(i);
                    values.add(cell == null ? "" : readCell(cell, formatter));
                }
                if (values.stream().allMatch(v -> v == null || v.isBlank())) continue;

                if (headers.isEmpty()) {
                    headers.addAll(values);
                    continue;
                }
                if (rows.size() >= maxRows) break;
                rows.add(values);
            }
            return new Sheet2D(headers, rows);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            // Covers a zip bomb tripping the inflate-ratio guard, a corrupt
            // archive, and a .xlsm renamed to .xlsx.
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Could not read this Excel file: " + e.getMessage());
        }
    }

    private static String readCell(Cell cell, DataFormatter formatter) {
        if (cell.getCellType() == CellType.FORMULA) {
            // Never evaluate. Use whatever value Excel last cached, and fall
            // back to the formula text so the row is still recognisable.
            try {
                return formatter.formatCellValue(cell);
            } catch (Exception e) {
                return cell.getCellFormula();
            }
        }
        return formatter.formatCellValue(cell);
    }
}
