/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amodeus.analysis.report;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.StackedDistanceChartImage;
import ch.ethz.idsc.amodeus.analysis.element.DistanceElement;
import ch.ethz.idsc.amodeus.analysis.plot.CompositionStack;

public enum DistanceElementHtml implements HtmlReportElement {
    INSTANCE;

    private static final String IMAGE_FOLDER = "../data"; // relative to report folder
    private static final DecimalFormat DECIMAL = new DecimalFormat("#0.00");

    @Override
    public Map<String, HtmlBodyElement> process(AnalysisSummary analysisSummary) {
        DistanceElement de = analysisSummary.getDistanceElement();
        Map<String, HtmlBodyElement> bodyElements = new HashMap<>();
        int numbVerhicles = analysisSummary.getSimulationInformationElement().vehicleSize();
        // Aggregated Results:
        String aRKey = BodyElementKeys.AGGREGATERESULTS;
        HtmlBodyElement aRElement = new HtmlBodyElement();
        aRElement.getHTMLGenerator();
        aRElement.getHTMLGenerator().insertTextLeft("\nDistance Ratio:" + //
                "\nOccupancy Ratio:" + //
                "\n" + //
                "\n" + HtmlGenerator.bold("Distances") + //
                "\n\tTotal:" + //
                "\n\tRebalancing:" + //
                "\n\tPickup:" + //
                "\n\tParking:" + //
                "\n\tEmpty Distance:" + //
                "\n\tWith Customer:" + //
                "\n" + //
                "\nMaximum Vehicle Total Distance:" + //
                "\nMinimum Vehicle Total Distance:" + //
                "\nMean Vehicle Total Distance:" + //
                "\n" + //
                "\nAverage Trip Distance:" //
        );
        aRElement.getHTMLGenerator().insertTextLeft("\n" + DECIMAL.format(de.totalDistanceRatio * 100) + "%" + //
                "\n" + DECIMAL.format(de.avgOccupancy * 100) + " %" + //
                "\n\n" + //
                "\n" + DECIMAL.format(de.totalDistance) + " km" + //
                "\n" + DECIMAL.format(de.totalDistanceRebal) + " km (" + //
                DECIMAL.format(100 * de.totalDistanceRebal / de.totalDistance) + "%)" + //
                "\n" + DECIMAL.format(de.totalDistancePicku) + " km (" + //
                DECIMAL.format(100 * de.totalDistancePicku / de.totalDistance) + "%)" + //
                "\n" + DECIMAL.format(de.totalDistanceParki) + " km (" + //
                DECIMAL.format(100 * de.totalDistanceParki / de.totalDistance) + "%)" + //
                "\n" + DECIMAL.format(de.totalDistance-de.totalDistanceWtCst) + " km (" + //
                DECIMAL.format(100 * (de.totalDistance-de.totalDistanceWtCst) / de.totalDistance) + "%)" + //
                "\n" + DECIMAL.format(de.totalDistanceWtCst) + " km (" + //
                DECIMAL.format(100 * de.totalDistanceWtCst / de.totalDistance) + "%)" + //
                "\n" + //
                "\n" + DECIMAL.format(de.maxDistanceOfVehicle) + " km" + //
                "\n" + DECIMAL.format(de.minDistanceOfVehicle) + " km" + //
                "\n" + DECIMAL.format(de.totalDistance/numbVerhicles) + " km" + //
                "\n" + //
                "\n" + DECIMAL.format(de.totalDistanceWtCst / de.requestIndices.size()) + " km");
        File img = new File(IMAGE_FOLDER, StackedDistanceChartImage.FILENAME + ".png");
        aRElement.getHTMLGenerator().insertImg(img.getPath(), CompositionStack.WIDTH, CompositionStack.HEIGHT);
        bodyElements.put(aRKey, aRElement);
        return bodyElements;
    }
}
