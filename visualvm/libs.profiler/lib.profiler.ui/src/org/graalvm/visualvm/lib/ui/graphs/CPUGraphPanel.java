/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.graalvm.visualvm.lib.ui.graphs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.Date;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;
import org.graalvm.visualvm.lib.charts.ChartItem;
import org.graalvm.visualvm.lib.charts.axis.AxisComponent;
import org.graalvm.visualvm.lib.charts.ChartSelectionModel;
import org.graalvm.visualvm.lib.charts.ItemsModel;
import org.graalvm.visualvm.lib.charts.swing.LongRect;
import org.graalvm.visualvm.lib.charts.swing.CrossBorderLayout;
import org.graalvm.visualvm.lib.charts.PaintersModel;
import org.graalvm.visualvm.lib.charts.xy.DecimalXYItemMarksComputer;
import org.graalvm.visualvm.lib.charts.axis.PercentLongMarksPainter;
import org.graalvm.visualvm.lib.charts.axis.TimeMarksPainter;
import org.graalvm.visualvm.lib.charts.axis.TimelineMarksComputer;
import org.graalvm.visualvm.lib.charts.xy.XYItem;
import org.graalvm.visualvm.lib.charts.xy.XYItemPainter;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItem;
import org.graalvm.visualvm.lib.jfluid.results.DataManagerListener;
import org.graalvm.visualvm.lib.jfluid.results.monitor.VMTelemetryDataManager;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.charts.xy.ProfilerXYChart;
import org.graalvm.visualvm.lib.ui.charts.xy.ProfilerXYItemPainter;
import org.graalvm.visualvm.lib.ui.charts.xy.ProfilerXYSelectionOverlay;
import org.graalvm.visualvm.lib.ui.charts.xy.ProfilerXYTooltipModel;
import org.graalvm.visualvm.lib.ui.charts.xy.ProfilerXYTooltipOverlay;
import org.graalvm.visualvm.lib.ui.charts.xy.ProfilerXYTooltipPainter;
import org.graalvm.visualvm.lib.ui.components.ColorIcon;
import org.graalvm.visualvm.lib.ui.monitor.VMTelemetryModels;
import org.graalvm.visualvm.lib.ui.swing.InvisibleToolbar;


/**
 *
 * @author Jiri Sedlacek
 */
public final class CPUGraphPanel extends GraphPanel {

    private ProfilerXYChart chart;
    private Action[] chartActions;

    private final VMTelemetryModels models;
    private final DataManagerListener listener;

    private final boolean smallPanel;
    
    private final Color CPU_COLOR = ColorFactory.getPredefinedColor(0);
    private final Color GC_COLOR = ColorFactory.getPredefinedColor(1);


    // --- Constructors --------------------------------------------------------

    public static CPUGraphPanel createBigPanel(VMTelemetryModels models) {
        return new CPUGraphPanel(models, false, null);
    }

    public static CPUGraphPanel createSmallPanel(VMTelemetryModels models,
                                             Action chartAction) {
        return new CPUGraphPanel(models, true, chartAction);
    }

    private CPUGraphPanel(VMTelemetryModels models,
                             boolean smallPanel, Action chartAction) {

        // Save models and panel type
        this.models = models;
        this.smallPanel = smallPanel;

        // Create UI
        initComponents(chartAction);

        // Register listener
        listener = new DataManagerListener() {
            public void dataChanged() { updateData(); }
            public void dataReset() { resetData(); }
        };
        models.getDataManager().addDataListener(listener);

        // Initialize chart & legend
        resetData();
    }


    // --- GraphPanel implementation -------------------------------------------

    public Action[] getActions() {
        return chartActions;
    }
    
    public void cleanup() {
        models.getDataManager().removeDataListener(listener);
    }


    // --- Private implementation ----------------------------------------------

    private void updateData() {
        if (smallPanel) {
            if (chart.fitsWidth()) {
                VMTelemetryDataManager manager = models.getDataManager();
                long[] timestamps = manager.timeStamps;
                if (timestamps[manager.getItemCount() - 1] - timestamps[0] >=
                    SMALL_CHART_FIT_TO_WINDOW_PERIOD)
                        chart.setFitsWidth(false);
            }
        } else {
        }
    }

    private void resetData() {
        if (smallPanel) {
            chart.setScale(INITIAL_CHART_SCALEX, 1);
            chart.setOffset(0, 0);
            chart.setFitsWidth(true);
        } else {
            chart.setScale(INITIAL_CHART_SCALEX, 1);
            chart.setOffset(0, 0);
            chart.setFitsWidth(false);
        }
        chart.setInitialDataBounds(new LongRect(System.currentTimeMillis(), 0,
                                       2500, 1000));
    }


    private void initComponents(final Action chartAction) {
        // Painters model
        PaintersModel paintersModel = createGenerationsPaintersModel();

        // Chart
        chart = createChart(models.cpuItemsModel(),
                            paintersModel, smallPanel);
        chart.setBackground(GraphsUI.CHART_BACKGROUND_COLOR);
        chart.setViewInsets(new Insets(10, 0, 0, 0));

        // Horizontal axis
        AxisComponent hAxis =
                new AxisComponent(chart, new TimelineMarksComputer(
                         models.cpuItemsModel().getTimeline(),
                         chart.getChartContext(), SwingConstants.HORIZONTAL),
                         new TimeMarksPainter(),
                         SwingConstants.NORTH, AxisComponent.MESH_FOREGROUND);
        hAxis.setForeground(Color.GRAY);

        // CPU time axis
        XYItem cpuTimeItem = models.cpuItemsModel().getItem(0);
        XYItemPainter cpuTimePainter = (XYItemPainter)paintersModel.getPainter(cpuTimeItem);
        PercentLongMarksPainter cpuTimeMarksPainter = new PercentLongMarksPainter(0, 1000);
        AxisComponent cAxis =
                new AxisComponent(chart, new DecimalXYItemMarksComputer(
                         cpuTimeItem, cpuTimePainter, chart.getChartContext(),
                         SwingConstants.VERTICAL),
                         cpuTimeMarksPainter, SwingConstants.WEST,
                         AxisComponent.MESH_FOREGROUND);
        cAxis.setForeground(Color.GRAY);

        // Chart panel (chart & axes)
        JPanel chartPanel = new JPanel(new CrossBorderLayout());
        chartPanel.setBackground(GraphsUI.CHART_BACKGROUND_COLOR);
        chartPanel.setBorder(BorderFactory.createMatteBorder(
                             10, 10, 5, 5, GraphsUI.CHART_BACKGROUND_COLOR));
        chartPanel.add(chart, new Integer[] { SwingConstants.CENTER });
        chartPanel.add(hAxis, new Integer[] { SwingConstants.NORTH,
                                              SwingConstants.NORTH_EAST,
                                              SwingConstants.NORTH_WEST });
        chartPanel.add(cAxis, new Integer[] { SwingConstants.WEST,
                                              SwingConstants.SOUTH_WEST });
        
        JScrollBar scroller = new JScrollBar(JScrollBar.HORIZONTAL);
        chart.attachHorizontalScrollBar(scroller);
        chartPanel.add(scroller, new Integer[] { SwingConstants.SOUTH });

        // Small panel UI
        if (smallPanel) {

        // Big panel UI
        } else {
            
            // Tooltip support
            ProfilerXYTooltipPainter tooltipPainter = new ProfilerXYTooltipPainter(createTooltipModel());
            chart.addOverlayComponent(new ProfilerXYTooltipOverlay(chart, tooltipPainter));
            chart.getSelectionModel().setHoverMode(ChartSelectionModel.HOVER_EACH_NEAREST);

            // Hovering support
            ProfilerXYSelectionOverlay selectionOverlay = new ProfilerXYSelectionOverlay();
            chart.addOverlayComponent(selectionOverlay);
            selectionOverlay.registerChart(chart);
            chart.getSelectionModel().setMoveMode(ChartSelectionModel.SELECTION_LINE_V);

            // Chart container (chart panel & scrollbar)
            JPanel chartContainer = new JPanel(new BorderLayout());
            chartContainer.setBorder(BorderFactory.createEmptyBorder());
            chartContainer.add(chartPanel, BorderLayout.CENTER);
            
            
            // Side panel
            JPanel sidePanel = new JPanel(new BorderLayout());
            sidePanel.setOpaque(false);
            int h = new JLabel("XXX").getPreferredSize().height; // NOI18N
            sidePanel.setBorder(BorderFactory.createEmptyBorder(h + 17, 0, 0, 10));
            InvisibleToolbar toolbar = new InvisibleToolbar(InvisibleToolbar.VERTICAL);
            toolbar.setOpaque(true);
            toolbar.setBackground(UIUtils.getProfilerResultsBackground());
            toolbar.add(chart.toggleViewAction()).setBackground(UIUtils.getProfilerResultsBackground());
            toolbar.add(chart.zoomInAction()).setBackground(UIUtils.getProfilerResultsBackground());
            toolbar.add(chart.zoomOutAction()).setBackground(UIUtils.getProfilerResultsBackground());
            sidePanel.add(toolbar, BorderLayout.CENTER);            

            // Heap Size
            JLabel heapSizeBig = new JLabel(GraphsUI.CPU_TIME_NAME,
                                            new ColorIcon(CPU_COLOR, Color.
                                            BLACK, 18, 9), SwingConstants.LEADING);
            heapSizeBig.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

            // Used heap
            JLabel usedHeapBig = new JLabel(GraphsUI.GC_TIME_NAME,
                                            new ColorIcon(GC_COLOR, Color.
                                            BLACK, 18, 9), SwingConstants.LEADING);
            usedHeapBig.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

            // Legend container
            JPanel bigLegendPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 7, 0));
            bigLegendPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 30));
            bigLegendPanel.setOpaque(true);
            bigLegendPanel.setBackground(UIUtils.getProfilerResultsBackground());
            bigLegendPanel.add(heapSizeBig);
            bigLegendPanel.add(usedHeapBig);

            // Master UI
            setLayout(new BorderLayout());
            setBackground(UIUtils.getProfilerResultsBackground());
            JLabel caption = new JLabel(GraphsUI.CPU_GC_CAPTION, JLabel.CENTER);
            caption.setFont(caption.getFont().deriveFont(Font.BOLD));
            caption.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
            caption.setOpaque(true);
            caption.setBackground(UIUtils.getProfilerResultsBackground());
            add(caption, BorderLayout.NORTH);
            add(chartContainer, BorderLayout.CENTER);
            add(bigLegendPanel, BorderLayout.SOUTH);
            add(sidePanel, BorderLayout.EAST);


            // Toolbar actions
            chartActions = new Action[] { chart.zoomInAction(),
                                          chart.zoomOutAction(),
                                          chart.toggleViewAction()};

        }

    }

    protected ProfilerXYTooltipModel createTooltipModel() {
        return new ProfilerXYTooltipModel() {

            public String getTimeValue(long timestamp) {
                return DATE_FORMATTER.format(new Date(timestamp));
            }

            public int getRowsCount() {
                return 2;
            }

            public String getRowName(int index) {
                switch (index) {
                    case 0:
                        return GraphsUI.CPU_TIME_NAME;
                    case 1:
                        return GraphsUI.GC_TIME_NAME;
                    default:
                        return null;
                }
            }

            public Color getRowColor(int index) {
                switch (index) {
                    case 0:
                        return GraphsUI.PROFILER_RED;
                    case 1:
                        return GraphsUI.GC_TIME_PAINTER_LINE_COLOR;
                    default:
                        return null;
                }
            }

            public String getRowValue(int index, long itemValue) {
                if (itemValue < 0) return "N/A"; // NOI18N
                String val = PERCENT_FORMATTER.format(itemValue / 1000f);
                return trimPercents(val);
            }

            public String getRowUnits(int index) {
                return "%"; // NOI18N
            }

            public int getExtraRowsCount() {
                return getRowsCount();
            }

            public String getExtraRowName(int index) {
                return getMaxValueString(getRowName(index));
            }

            public Color getExtraRowColor(int index) {
                return getRowColor(index);
            }

            public String getExtraRowValue(int index) {
                SynchronousXYItem item = models.cpuItemsModel().getItem(index);
                long maxValue = item.getMaxYValue();
                if (maxValue < 0) return "N/A"; // NOI18N
                String val = PERCENT_FORMATTER.format(maxValue / 1000f);
                return trimPercents(val);
            }

            public String getExtraRowUnits(int index) {
                return getRowUnits(index);
            }

            private String trimPercents(String percents) {
                return !percents.endsWith("%") ? percents : // NOI18N
                        percents.substring(0, percents.length() - 1).trim();
            }

        };
    }

    private PaintersModel createGenerationsPaintersModel() {
        // CPU
        ProfilerXYItemPainter cpuTimePainter =
                ProfilerXYItemPainter.relativePainter(GraphsUI.GC_TIME_PAINTER_LINE_WIDTH,
                                                      CPU_COLOR,
                                                      null,
                                                      10);
        XYItemPainter ctp = cpuTimePainter;

        // Relative time spent in GC
        ProfilerXYItemPainter gcTimePainter =
                ProfilerXYItemPainter.relativePainter(GraphsUI.GC_TIME_PAINTER_LINE_WIDTH,
                                                      GC_COLOR,
                                                      null,
                                                      10);
        XYItemPainter gtp = gcTimePainter;

        // Model
        ItemsModel items = models.cpuItemsModel();
        PaintersModel model = new PaintersModel.Default(
                                            new ChartItem[] { items.getItem(0),
                                                              items.getItem(1) },
                                            new XYItemPainter[] { ctp, gtp });

        return model;
    }

}
