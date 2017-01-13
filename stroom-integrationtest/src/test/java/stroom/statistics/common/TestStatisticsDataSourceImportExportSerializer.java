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

package stroom.statistics.common;

import stroom.AbstractCoreIntegrationTest;
import stroom.entity.shared.BaseResultList;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.FindFolderCriteria;
import stroom.entity.shared.FolderService;
import stroom.importexport.server.ImportExportSerializer;
import stroom.importexport.server.ImportExportSerializer.ImportMode;
import stroom.query.shared.DataSource;
import stroom.statistics.server.common.StatisticsDataSourceProvider;
import stroom.statistics.shared.StatisticField;
import stroom.statistics.shared.StatisticStore;
import stroom.statistics.shared.StatisticStoreEntity;
import stroom.statistics.shared.StatisticType;
import stroom.statistics.shared.StatisticsDataSourceData;
import stroom.streamstore.server.fs.FileSystemUtil;
import stroom.util.test.FileSystemTestUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;
import java.io.File;

public class TestStatisticsDataSourceImportExportSerializer extends AbstractCoreIntegrationTest {
    @Resource
    private ImportExportSerializer importExportSerializer;
    @Resource
    private FolderService folderService;
    @Resource
    private StatisticStoreService statisticsDataSourceService;
    @Resource
    private StatisticsDataSourceProvider statisticsDataSourceProvider;

    private FindFolderCriteria buildFindFolderCriteria() {
        final FindFolderCriteria criteria = new FindFolderCriteria();
        criteria.getFolderIdSet().setDeep(true);
        criteria.getFolderIdSet().setMatchNull(Boolean.TRUE);
        return criteria;
    }

    /**
     * Create a populated {@link StatisticStore} object, serialise it to file,
     * de-serialise it back to an object then compare the first object with the
     * second one
     */
    @Test
    public void testStatisticsDataSource() {
        final DocRef folder = DocRef.create(folderService.create(null, FileSystemTestUtil.getUniqueTestString()));

        final StatisticStoreEntity statisticsDataSource = statisticsDataSourceService.create(folder, "StatName1");
        statisticsDataSource.setEngineName("EngineName1");
        statisticsDataSource.setDescription("My Description");
        statisticsDataSource.setStatisticType(StatisticType.COUNT);
        statisticsDataSource.setStatisticDataSourceDataObject(new StatisticsDataSourceData());
        statisticsDataSource.getStatisticDataSourceDataObject().addStatisticField(new StatisticField("tag1"));
        statisticsDataSource.getStatisticDataSourceDataObject().addStatisticField(new StatisticField("tag2"));
        statisticsDataSourceService.save(statisticsDataSource);

        Assert.assertEquals(1, statisticsDataSourceService.find(FindStatisticsEntityCriteria.instance()).size());

        final File testDataDir = new File(getCurrentTestDir(), "ExportTest");

        FileSystemUtil.deleteDirectory(testDataDir);
        FileSystemUtil.mkdirs(null, testDataDir);

        importExportSerializer.write(testDataDir, buildFindFolderCriteria(), true, false, null);

        Assert.assertEquals(2, testDataDir.listFiles().length);

        // now clear out the java entities and import from file
        clean(true);

        Assert.assertEquals(0, statisticsDataSourceService.find(FindStatisticsEntityCriteria.instance()).size());

        importExportSerializer.read(testDataDir, null, ImportMode.IGNORE_CONFIRMATION);

        final BaseResultList<StatisticStoreEntity> dataSources = statisticsDataSourceService
                .find(FindStatisticsEntityCriteria.instance());

        Assert.assertEquals(1, dataSources.size());

        final StatisticStoreEntity importedDataSource = dataSources.get(0);

        Assert.assertEquals(statisticsDataSource.getName(), importedDataSource.getName());
        Assert.assertEquals(statisticsDataSource.getEngineName(), importedDataSource.getEngineName());
        Assert.assertEquals(statisticsDataSource.getStatisticType(), importedDataSource.getStatisticType());
        Assert.assertEquals(statisticsDataSource.getDescription(), importedDataSource.getDescription());

        Assert.assertEquals(statisticsDataSource.getStatisticDataSourceDataObject(),
                importedDataSource.getStatisticDataSourceDataObject());
    }

    @Test
    public void testDeSerialiseOnLoad() {
        final DocRef folder = DocRef.create(folderService.create(null, FileSystemTestUtil.getUniqueTestString()));

        final StatisticStoreEntity statisticsDataSource = statisticsDataSourceService.create(folder, "StatName1");
        statisticsDataSource.setEngineName("EngineName1");
        statisticsDataSource.setDescription("My Description");
        statisticsDataSource.setStatisticType(StatisticType.COUNT);

        statisticsDataSource.setStatisticDataSourceDataObject(new StatisticsDataSourceData());
        statisticsDataSource.getStatisticDataSourceDataObject().addStatisticField(new StatisticField("tag1"));
        statisticsDataSource.getStatisticDataSourceDataObject().addStatisticField(new StatisticField("tag2"));

        statisticsDataSourceService.save(statisticsDataSource);

        StatisticStoreEntity statisticsDataSource2 = statisticsDataSourceService
                .find(FindStatisticsEntityCriteria.instance()).getFirst();

        final String uuid = statisticsDataSource2.getUuid();

        statisticsDataSource2 = null;

        final StatisticStoreEntity statisticsDataSource3 = statisticsDataSourceService.loadByUuid(uuid);

        // Assert.assertNotNull(((StatisticsDataSource)
        // statisticsDataSource3).getStatisticDataSourceData());
        Assert.assertNotNull(statisticsDataSource3.getStatisticDataSourceDataObject());

        final DataSource statisticsDataSource4 = statisticsDataSourceProvider.getDataSource(uuid);

        Assert.assertNotNull(statisticsDataSource4.getIndexFieldsObject());
    }
}
