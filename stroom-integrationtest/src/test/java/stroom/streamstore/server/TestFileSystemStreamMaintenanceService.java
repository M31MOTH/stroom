/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.streamstore.server;

import org.junit.Assert;
import org.junit.Test;
import stroom.AbstractCoreIntegrationTest;
import stroom.CommonTestScenarioCreator;
import stroom.feed.shared.Feed;
import stroom.jobsystem.server.MockTask;
import stroom.streamstore.shared.Stream;
import stroom.streamstore.shared.StreamType;
import stroom.streamtask.server.FileSystemCleanExecutor;
import stroom.util.io.FileUtil;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestFileSystemStreamMaintenanceService extends AbstractCoreIntegrationTest {
    @Resource
    private StreamMaintenanceService streamMaintenanceService;
    @Resource
    private CommonTestScenarioCreator commonTestScenarioCreator;
    @Resource
    private FileSystemCleanExecutor fileSystemCleanTaskExecutor;

    @Test
    public void testSimple() throws IOException {
        // commonTestControl.deleteAll();

        final Feed eventFeed = commonTestScenarioCreator.createSimpleFeed();

        final Stream md = commonTestScenarioCreator.createSample2LineRawFile(eventFeed, StreamType.RAW_EVENTS);

        commonTestScenarioCreator.createSampleBlankProcessedFile(eventFeed, md);

        final List<File> files = streamMaintenanceService.findAllStreamFile(md);

        Assert.assertTrue(files.size() > 0);

        final String path = files.get(0).getParent();
        final String volPath = path.substring(path.indexOf("RAW_EVENTS"));

        final StreamRange streamRange = new StreamRange(volPath);
        final FindStreamVolumeCriteria findStreamVolumeCriteria = new FindStreamVolumeCriteria();
        findStreamVolumeCriteria.setStreamRange(streamRange);
        Assert.assertTrue(streamMaintenanceService.find(findStreamVolumeCriteria).size() > 0);

        final File dir = files.iterator().next().getParentFile();

        final File test1 = new File(dir, "badfile.dat");

        FileUtil.createNewFile(test1);

        fileSystemCleanTaskExecutor.exec(new MockTask("Test"));
    }

    // @Test
    // public void testChartQuery() throws IOException {
    //
    // FindStreamChartCriteria findStreamChartCriteria = new
    // FindStreamChartCriteria();
    // findStreamChartCriteria.setPeriod(new Period(0L, Long.MAX_VALUE));
    //
    // ResultList<SharedString> chartList = streamMaintenanceService
    // .getChartList(findStreamChartCriteria);
    // ChartCriteria chartCriteria = new ChartCriteria();
    // for (SharedString chartType : chartList) {
    // chartCriteria.add(chartType.toString(), ChartFunc.MAX);
    // chartCriteria.add(chartType.toString(), ChartFunc.MIN);
    // chartCriteria.add(chartType.toString(), ChartFunc.AVG);
    // }
    // findStreamChartCriteria.setChartCriteria(chartCriteria);
    // streamMaintenanceService.getChartData(findStreamChartCriteria);
    // }
}
