package com.nagrikHelp.service;

import com.nagrikHelp.model.Issue;
import com.nagrikHelp.model.IssueStatus;
import com.nagrikHelp.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final IssueRepository issueRepository;

    public byte[] generateMonthlyResolvedPdf(int year, int month) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        Date startDate = Date.from(start.atStartOfDay(zone).toInstant());
        Date endDate = Date.from(end.atStartOfDay(zone).toInstant());

        // Data queries
        List<Issue> resolved = issueRepository.findByStatusAndUpdatedAtBetween(IssueStatus.RESOLVED, startDate, endDate);
        long startMillis = startDate.getTime();
        long endMillis = endDate.getTime();
        List<Issue> created = issueRepository.findByCreatedAtBetween(startMillis, endMillis);

        // Daily counts (resolved)
        Map<LocalDate, Long> resolvedCounts = resolved.stream().collect(Collectors.groupingBy(i -> i.getUpdatedAt().toInstant().atZone(zone).toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> createdCounts = created.stream().collect(Collectors.groupingBy(i -> new Date(i.getCreatedAt()).toInstant().atZone(zone).toLocalDate(), Collectors.counting()));
        List<String> dayLabels = new ArrayList<>();
        List<Long> resolvedSeries = new ArrayList<>();
        List<Long> createdSeries = new ArrayList<>();
        for (LocalDate d = start; d.isBefore(end); d = d.plusDays(1)) {
            dayLabels.add(String.valueOf(d.getDayOfMonth()));
            resolvedSeries.add(resolvedCounts.getOrDefault(d, 0L));
            createdSeries.add(createdCounts.getOrDefault(d, 0L));
        }

        // Build chart
        CategoryChart chart = new CategoryChartBuilder()
                .width(800).height(400)
                .title("Monthly Issues - Created vs Resolved")
                .xAxisTitle("Day")
                .yAxisTitle("Count")
                .build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.addSeries("Created", dayLabels, createdSeries);
        chart.addSeries("Resolved", dayLabels, resolvedSeries);

        // Status distribution among created issues in this month
        Map<IssueStatus, Long> statusDistribution = created.stream().collect(Collectors.groupingBy(Issue::getStatus, Collectors.counting()));
        // Category distribution among created issues (ignore null)
        Map<String, Long> categoryDistribution = created.stream()
                .filter(i -> i.getCategory() != null)
                .collect(Collectors.groupingBy(i -> i.getCategory().name(), Collectors.counting()));
        // Build status pie chart
        PieChart statusPie = new PieChartBuilder().width(450).height(350).title("Status Distribution").build();
        statusPie.getStyler().setLegendVisible(true);
        statusDistribution.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> statusPie.addSeries(e.getKey().name(), e.getValue()));
        // Build category bar chart (top categories)
        CategoryChart catChart = new CategoryChartBuilder().width(450).height(350).title("Top Categories (Created)").xAxisTitle("Category").yAxisTitle("Count").build();
        catChart.getStyler().setLegendVisible(false);
        List<Map.Entry<String, Long>> catSorted = categoryDistribution.entrySet().stream()
                .sorted((a,b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(8).toList();
        if(!catSorted.isEmpty()) {
            List<String> catNames = catSorted.stream().map(Map.Entry::getKey).toList();
            List<Long> catCounts = catSorted.stream().map(Map.Entry::getValue).toList();
            catChart.addSeries("Categories", catNames, catCounts);
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 40f;
                float y = page.getMediaBox().getHeight() - margin;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(margin, y);
                cs.showText("Monthly Issue Report - " + year + String.format("-%02d", month));
                cs.endText();
                y -= 30;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Created this month: " + created.size() + ", Resolved (updated) this month: " + resolved.size());
                cs.endText();
                y -= 20;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Status distribution (created this month): " + statusDistribution.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> e.getKey() + ": " + e.getValue())
                        .collect(Collectors.joining(", ")));
                cs.endText();
                y -= 220; // reserve space above chart

                // Draw chart image
                BufferedImage img = BitmapEncoder.getBufferedImage(chart);
                var pdImage = LosslessFactory.createFromImage(doc, img);
                float chartWidth = page.getMediaBox().getWidth() - 2 * margin;
                float scale = chartWidth / img.getWidth();
                float chartHeight = img.getHeight() * scale;
                cs.drawImage(pdImage, margin, y - chartHeight + 160, chartWidth, chartHeight); // adjust vertical placement
                y -= chartHeight + 10;
            }

            // Second page with detailed table (simple text list up to first 25 issues created)
            PDPage chartsPage = new PDPage(PDRectangle.LETTER);
            doc.addPage(chartsPage);
            try (PDPageContentStream csCharts = new PDPageContentStream(doc, chartsPage)) {
                float margin = 40f;
                float y = chartsPage.getMediaBox().getHeight() - margin;
                csCharts.beginText();
                csCharts.setFont(PDType1Font.HELVETICA_BOLD, 16);
                csCharts.newLineAtOffset(margin, y);
                csCharts.showText("Distributions");
                csCharts.endText();
                y -= 20;
                // Render charts side by side if space allows
                BufferedImage pieImg = BitmapEncoder.getBufferedImage(statusPie);
                BufferedImage catImg = BitmapEncoder.getBufferedImage(catChart);
                float availableWidth = chartsPage.getMediaBox().getWidth() - 2 * margin;
                float gap = 20f;
                float halfWidth = (availableWidth - gap) / 2f;
                // Scale preserving aspect ratio
                float pieScale = halfWidth / pieImg.getWidth();
                float pieHeight = pieImg.getHeight() * pieScale;
                float catScale = halfWidth / catImg.getWidth();
                float catHeight = catImg.getHeight() * catScale;
                float rowHeight = Math.max(pieHeight, catHeight);
                var piePd = LosslessFactory.createFromImage(doc, pieImg);
                var catPd = LosslessFactory.createFromImage(doc, catImg);
                csCharts.drawImage(piePd, margin, y - rowHeight, halfWidth, pieHeight);
                csCharts.drawImage(catPd, margin + halfWidth + gap, y - rowHeight, halfWidth, catHeight);
            }

            PDPage page2 = new PDPage(PDRectangle.LETTER);
            doc.addPage(page2);
            try (PDPageContentStream cs2 = new PDPageContentStream(doc, page2)) {
                float margin = 40f;
                float y = page2.getMediaBox().getHeight() - margin;
                cs2.beginText();
                cs2.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs2.newLineAtOffset(margin, y);
                cs2.showText("Issue List (Created This Month) - First 25");
                cs2.endText();
                y -= 24;
                cs2.setFont(PDType1Font.HELVETICA, 9);
                int idx = 1;
                for (Issue i : created.stream().sorted(Comparator.comparingLong(Issue::getCreatedAt)).limit(25).toList()) {
                    if (y < 60) { // new page
                        cs2.close();
                        PDPage extra = new PDPage(PDRectangle.LETTER);
                        doc.addPage(extra);
                        y = extra.getMediaBox().getHeight() - margin;
                        try (PDPageContentStream nx = new PDPageContentStream(doc, extra)) {
                            nx.beginText();
                            nx.setFont(PDType1Font.HELVETICA_BOLD, 16);
                            nx.newLineAtOffset(margin, y);
                            nx.showText("Issue List Continued");
                            nx.endText();
                            y -= 20;
                        }
                        break; // keep implementation simple (not continuing list fully)
                    }
                    String line = String.format(Locale.ROOT, "%02d. %s | %s | %s", idx++, truncate(i.getTitle(), 40), i.getStatus(), new Date(i.getCreatedAt()));
                    cs2.beginText();
                    cs2.newLineAtOffset(margin, y);
                    cs2.showText(line);
                    cs2.endText();
                    y -= 14;
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed generating PDF", e);
        }
    }

    private String truncate(String v, int max) {
        if (v == null) return "";
        return v.length() <= max ? v : v.substring(0, max - 3) + "...";
    }
}
