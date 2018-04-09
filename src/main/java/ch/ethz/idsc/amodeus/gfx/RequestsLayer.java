/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amodeus.gfx;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;

import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.amodeus.net.OsmLink;
import ch.ethz.idsc.amodeus.net.RequestContainer;
import ch.ethz.idsc.amodeus.net.SimulationObject;
import ch.ethz.idsc.amodeus.util.gui.RowPanel;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.amodeus.view.gheat.gui.ColorSchemes;
import ch.ethz.idsc.amodeus.view.jmapviewer.AmodeusHeatMap;

public class RequestsLayer extends ViewerLayer {

    private static final Font REQUESTS_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    // ---
    private final AmodeusHeatMap requestHeatMap = new AmodeusHeatMapImpl(ColorSchemes.OrangeContour);
    private final AmodeusHeatMap requestDestMap = new AmodeusHeatMapImpl(ColorSchemes.GreenContour);

    private volatile boolean maxWaitTimeInHud = true;
    private volatile boolean drawNumber = true;
    private volatile boolean drawRequestDestinations = false;

    private double maxWaitTime;

    public RequestsLayer() {
        requestDestMap.setShow(false); // default: don't show distrib of request dest
    }

    @Override
    public void prepareHeatmaps(SimulationObject ref) {
        {
            requestHeatMap.clear();
            Map<Integer, List<RequestContainer>> map = ref.requests.stream() //
                    .collect(Collectors.groupingBy(requestContainer -> requestContainer.fromLinkIndex));
            for (Entry<Integer, List<RequestContainer>> entry : map.entrySet()) {
                // all streets have positive indexes
                GlobalAssert.that(entry.getKey() >= 0);
                OsmLink osmLink = amodeusComponent.db.getOsmLink(entry.getKey());
                final int size = entry.getValue().size();
                for (int count = 0; count < size; ++count) {
                    Coord coord = osmLink.getAt(count / (double) size);
                    requestHeatMap.addPoint(coord.getX(), coord.getY());

                }
            }
        }
        // ---
        {
            requestDestMap.clear();
            Map<Integer, List<RequestContainer>> map = ref.requests.stream() //
                    .collect(Collectors.groupingBy(requestContainer -> requestContainer.toLinkIndex));
            for (Entry<Integer, List<RequestContainer>> entry : map.entrySet()) {
                // all streets have positive indexes
                GlobalAssert.that(entry.getKey() >= 0);
                OsmLink osmLink = amodeusComponent.db.getOsmLink(entry.getKey());
                final int size = entry.getValue().size();
                for (int count = 0; count < size; ++count) {
                    Coord coord = osmLink.getAt(count / (double) size);
                    requestDestMap.addPoint(coord.getX(), coord.getY());

                }
            }
        }

    }

    @Override
    protected void paint(Graphics2D graphics, SimulationObject ref) {
        if (Objects.isNull(ref))
            return;

        maxWaitTime = 0;
        // draw requests
        graphics.setFont(REQUESTS_FONT);
        final boolean showNumbers = drawNumber && 13 < amodeusComponent.getZoom();
        Map<Integer, List<RequestContainer>> map = ref.requests.stream() //
                .collect(Collectors.groupingBy(requestContainer -> requestContainer.fromLinkIndex));
        for (Entry<Integer, List<RequestContainer>> entry : map.entrySet()) {
            Point p1;
            {
                int linkId = entry.getKey();
                OsmLink osmLink = amodeusComponent.db.getOsmLink(linkId);
                p1 = amodeusComponent.getMapPosition(osmLink.getAt(0.5));
            }
            if (p1 != null) {
                final int numRequests = entry.getValue().size();

                final int x = p1.x;
                final int y = p1.y;

                {
                    graphics.setColor(new Color(32, 128, 32, 128));
                    @SuppressWarnings("unused")
                    int index = numRequests;
                    for (RequestContainer rc : entry.getValue()) {
                        double waitTime = ref.now - rc.submissionTime;
                        if (waitTime > maxWaitTime)
                            // System.out.println("Request Nr. " + rc.requestIndex + " waiting Time: " + waitTime);
                            maxWaitTime = Math.max(waitTime, maxWaitTime);
                        // int piy = y - index;
                        // int wid = (int) waitTime / 10;
                        // int left = x - wid / 2;
                        // graphics.drawLine(left, piy, left + wid, piy);
                        --index;
                    }
                }
                if (drawRequestDestinations) {
                    graphics.setColor(new Color(128, 128, 128, 64));
                    for (RequestContainer rc : entry.getValue()) {
                        int linkId = rc.toLinkIndex;
                        OsmLink osmLink = amodeusComponent.db.getOsmLink(linkId);
                        Point p2 = amodeusComponent.getMapPositionAlways(osmLink.getAt(0.5));
                        graphics.drawLine(x, y, p2.x, p2.y);
                    }
                }
                if (showNumbers) {
                    graphics.setColor(Color.GRAY);
                    graphics.drawString("" + numRequests, x, y); // - numRequests
                }
            }
        }
    }

    @Override
    protected void hud(Graphics2D graphics, SimulationObject ref) {
        if (ref != null) {
            InfoString infoString = new InfoString(String.format("%5d %s", ref.requests.size(), "open requests"));
            infoString.color = Color.BLACK; // new Color(204, 122, 0);
            amodeusComponent.append(infoString);
        }
        if (maxWaitTimeInHud) {
            InfoString infoString = new InfoString(String.format("%5d %s", Math.round(maxWaitTime / 60), "maxWaitTime [min]"));
            infoString.color = Color.BLACK; // new Color(255, 102, 0);
            amodeusComponent.append(infoString);
        }
        if (ref != null)
            amodeusComponent.append("%5d %s", ref.total_matchedRequests, "matched req.");
        amodeusComponent.appendSeparator();
    }

    public void setDrawDestinations(boolean selected) {
        drawRequestDestinations = selected;
        amodeusComponent.repaint();
    }

    public boolean getDrawDestinations() {
        return drawRequestDestinations;
    }

    @Override
    protected void createPanel(RowPanel rowPanel) {
        {
            final JCheckBox jCheckBox = new JCheckBox("number");
            jCheckBox.setToolTipText("exact number of people waiting (only for zoom > 13)");
            jCheckBox.setSelected(drawNumber);
            jCheckBox.addActionListener(event -> {
                drawNumber = jCheckBox.isSelected();
                amodeusComponent.repaint();
            });
            rowPanel.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("destin.");
            jCheckBox.setToolTipText("line of travel");
            jCheckBox.setSelected(getDrawDestinations());
            jCheckBox.addActionListener(event -> setDrawDestinations(jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
        createHeatmapPanel(rowPanel, "source", requestHeatMap);
        createHeatmapPanel(rowPanel, "sink", requestDestMap);
        {
            final JCheckBox jCheckBox = new JCheckBox("max. waittime");
            jCheckBox.setToolTipText("show max wait time in HUD");
            jCheckBox.setSelected(maxWaitTimeInHud);
            jCheckBox.addActionListener(event -> {
                maxWaitTimeInHud = jCheckBox.isSelected();
                amodeusComponent.repaint();
            });
            rowPanel.add(jCheckBox);
        }
    }

    @Override
    public List<AmodeusHeatMap> getHeatmaps() {
        return Arrays.asList(requestHeatMap, requestDestMap);
    }

}