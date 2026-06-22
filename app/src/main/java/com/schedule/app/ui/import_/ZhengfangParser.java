package com.schedule.app.ui.import_;

import com.schedule.app.util.CourseColorPalette;

import com.schedule.app.data.entity.Course;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses class schedule HTML from Zhengfang (正方) educational administration system.
 *
 * Zhengfang schedule tables typically have this structure:
 * - Table with id "table1" or the main schedule table
 * - First row: headers (节次, 星期一, 星期二, ...)
 * - Each subsequent row: section pair (1-2, 3-4, etc.)
 * - Each cell may contain multiple courses separated by <br> or line breaks
 * - Course info format: CourseName{WeekRange}{Teacher}{Location}
 *   or lines separated by <br>: CourseName / Teacher / WeekRange / Location
 */
public class ZhengfangParser {

    private final CourseColorPalette.Allocator colorAllocator = new CourseColorPalette.Allocator();

    public List<Course> parse(String html) {
        List<Course> courses = new ArrayList<>();
        if (html == null || html.isEmpty()) return courses;

        Document doc = Jsoup.parse(html);

        Element table = doc.getElementById("table1");
        if (table == null) {
            Elements tables = doc.select("table#Table1");
            if (!tables.isEmpty()) table = tables.first();
        }
        if (table == null) {
            Elements tables = doc.getElementsByTag("table");
            for (Element t : tables) {
                if (t.text().contains("星期一") || t.text().contains("Monday")) {
                    table = t;
                    break;
                }
            }
        }
        if (table == null) return courses;

        Elements rows = table.select("tr");
        int sectionBase = 1;

        for (int rowIdx = 1; rowIdx < rows.size(); rowIdx++) {
            Element row = rows.get(rowIdx);
            Elements cells = row.select("td");

            String firstCellText = cells.isEmpty() ? "" : cells.first().text().trim();
            int parsedSection = parseSectionFromLabel(firstCellText);
            if (parsedSection > 0) {
                sectionBase = parsedSection;
            }

            int dayOfWeek = 0;
            for (int cellIdx = 0; cellIdx < cells.size(); cellIdx++) {
                Element cell = cells.get(cellIdx);
                String cellText = cell.text().trim();

                if (cellIdx == 0 && (cellText.contains("节") || cellText.matches(".*\\d.*-.*\\d.*")
                        || cellText.isEmpty() || cellText.matches("^\\d+$"))) {
                    continue;
                }

                dayOfWeek++;
                if (dayOfWeek > 7) break;

                String cellHtml = cell.html();
                if (cellHtml.trim().isEmpty() || cellText.equals("\u00a0")) continue;

                int rowspan = 1;
                String rowspanAttr = cell.attr("rowspan");
                if (!rowspanAttr.isEmpty()) {
                    try { rowspan = Integer.parseInt(rowspanAttr); } catch (NumberFormatException ignored) {}
                }

                int endSection = sectionBase + rowspan - 1;
                List<Course> parsed = parseCellContent(cellHtml, dayOfWeek, sectionBase, endSection);
                courses.addAll(parsed);
            }

            sectionBase += getSectionIncrement(rows, rowIdx);
        }

        if (courses.isEmpty()) {
            courses = parseAlternativeFormat(doc);
        }

        return courses;
    }

    private List<Course> parseCellContent(String cellHtml, int dayOfWeek, int startSection, int endSection) {
        List<Course> results = new ArrayList<>();
        String text = Jsoup.parse(cellHtml).text().trim();
        if (text.isEmpty() || text.equals("\u00a0")) return results;

        String[] blocks = cellHtml.split("<br\\s*/?>\\s*<br\\s*/?>");
        if (blocks.length <= 1) {
            blocks = cellHtml.split("-----");
        }

        for (String block : blocks) {
            String cleanBlock = Jsoup.parse(block).text().trim();
            if (cleanBlock.isEmpty()) continue;

            Course course = parseSingleCourse(cleanBlock, dayOfWeek, startSection, endSection);
            if (course != null) results.add(course);
        }

        if (results.isEmpty() && !text.isEmpty()) {
            Course course = parseSingleCourse(text, dayOfWeek, startSection, endSection);
            if (course != null) results.add(course);
        }

        return results;
    }

    private Course parseSingleCourse(String text, int dayOfWeek, int startSection, int endSection) {
        if (text.isEmpty()) return null;

        String courseName = "";
        String teacher = "";
        String location = "";
        int startWeek = 1;
        int endWeek = 20;
        int weekType = 0;

        // Pattern: lines separated by spaces or known delimiters
        // Common formats:
        //   CourseName\nTeacher\n1-16周\nLocation
        //   CourseName(Teacher) 1-16周 Location
        String[] parts = text.split("[\\n\\r]+|\\s{2,}");
        if (parts.length == 1) {
            parts = text.split("\\s+");
        }

        // Try to extract week range: "1-16周" or "1-16周(单)" or "1,3,5,7周"
        Pattern weekPattern = Pattern.compile("(\\d+)-(\\d+)周(\\(([单双])\\))?");
        boolean weekFound = false;
        List<String> remainingParts = new ArrayList<>();

        for (String part : parts) {
            Matcher m = weekPattern.matcher(part);
            if (m.find() && !weekFound) {
                startWeek = Integer.parseInt(m.group(1));
                endWeek = Integer.parseInt(m.group(2));
                if (m.group(4) != null) {
                    weekType = m.group(4).equals("单") ? 1 : 2;
                }
                weekFound = true;
            } else {
                remainingParts.add(part.trim());
            }
        }

        if (!weekFound) {
            Pattern weekPattern2 = Pattern.compile("(\\d+)-(\\d+)");
            for (int i = remainingParts.size() - 1; i >= 0; i--) {
                Matcher m = weekPattern2.matcher(remainingParts.get(i));
                if (m.find() && remainingParts.get(i).contains("周")) {
                    startWeek = Integer.parseInt(m.group(1));
                    endWeek = Integer.parseInt(m.group(2));
                    remainingParts.remove(i);
                    weekFound = true;
                    break;
                }
            }
        }

        if (remainingParts.size() >= 3) {
            courseName = remainingParts.get(0);
            teacher = remainingParts.get(1);
            location = remainingParts.get(remainingParts.size() - 1);
        } else if (remainingParts.size() == 2) {
            courseName = remainingParts.get(0);
            location = remainingParts.get(1);
        } else if (remainingParts.size() == 1) {
            courseName = remainingParts.get(0);
        }

        if (courseName.isEmpty()) return null;

        int color = colorAllocator.getColor(courseName);

        return new Course(courseName, teacher, location,
                dayOfWeek, startSection, endSection,
                startWeek, endWeek, weekType, color);
    }

    private int parseSectionFromLabel(String label) {
        Pattern p = Pattern.compile("(\\d+)");
        Matcher m = p.matcher(label);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return -1;
    }

    private int getSectionIncrement(Elements rows, int currentRowIdx) {
        if (currentRowIdx + 1 < rows.size()) {
            Elements nextCells = rows.get(currentRowIdx + 1).select("td");
            if (!nextCells.isEmpty()) {
                String label = nextCells.first().text().trim();
                int next = parseSectionFromLabel(label);
                if (next > 0) {
                    String currLabel = rows.get(currentRowIdx).select("td").first().text().trim();
                    int curr = parseSectionFromLabel(currLabel);
                    if (curr > 0 && next > curr) return next - curr;
                }
            }
        }
        return 2;
    }

    /**
     * Alternative parsing for newer Zhengfang versions that use div-based layouts
     */
    private List<Course> parseAlternativeFormat(Document doc) {
        List<Course> courses = new ArrayList<>();

        Elements divCourses = doc.select("div.kbcontent, div[class*=course], td.kbcontent");
        for (Element div : divCourses) {
            String content = div.text().trim();
            if (content.isEmpty() || content.equals("\u00a0")) continue;

            Element parent = div.parent();
            int dayOfWeek = 1;
            int startSection = 1;
            int endSection = 2;

            String idAttr = div.attr("id");
            if (!idAttr.isEmpty()) {
                Pattern p = Pattern.compile("(\\d+)-(\\d+)-(\\d+)");
                Matcher m = p.matcher(idAttr);
                if (m.find()) {
                    dayOfWeek = Integer.parseInt(m.group(2));
                    startSection = Integer.parseInt(m.group(3));
                    endSection = startSection + 1;
                }
            }

            Course course = parseSingleCourse(content, dayOfWeek, startSection, endSection);
            if (course != null) courses.add(course);
        }

        return courses;
    }
}
