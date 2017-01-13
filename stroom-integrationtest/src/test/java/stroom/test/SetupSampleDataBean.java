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

package stroom.test;

import stroom.CommonTestControl;
import stroom.dashboard.shared.Dashboard;
import stroom.db.migration.mysql.V6_0_0_1__Explorer;
import stroom.entity.server.util.ConnectionUtil;
import stroom.entity.shared.BaseResultList;
import stroom.feed.shared.Feed;
import stroom.feed.shared.Feed.FeedStatus;
import stroom.feed.shared.FeedService;
import stroom.feed.shared.FindFeedCriteria;
import stroom.importexport.server.ImportExportSerializer;
import stroom.importexport.server.ImportExportSerializer.ImportMode;
import stroom.index.shared.FindIndexCriteria;
import stroom.index.shared.Index;
import stroom.index.shared.IndexService;
import stroom.jobsystem.shared.JobNodeService;
import stroom.jobsystem.shared.JobService;
import stroom.node.shared.FindVolumeCriteria;
import stroom.node.shared.Node;
import stroom.node.shared.Volume;
import stroom.node.shared.VolumeService;
import stroom.pipeline.shared.FindPipelineEntityCriteria;
import stroom.pipeline.shared.PipelineEntity;
import stroom.pipeline.shared.PipelineService;
import stroom.security.server.DBRealm;
import stroom.statistics.common.FindStatisticsEntityCriteria;
import stroom.statistics.common.StatisticStoreService;
import stroom.statistics.shared.StatisticStore;
import stroom.statistics.shared.StatisticStoreEntity;
import stroom.streamstore.server.StreamStore;
import stroom.streamstore.shared.FindStreamAttributeKeyCriteria;
import stroom.streamstore.shared.FindStreamCriteria;
import stroom.streamstore.shared.StreamAttributeConstants;
import stroom.streamstore.shared.StreamAttributeKey;
import stroom.streamstore.shared.StreamAttributeKeyService;
import stroom.streamstore.shared.StreamType;
import stroom.streamtask.shared.StreamProcessorFilterService;
import stroom.streamtask.shared.StreamProcessorService;
import stroom.util.io.StreamUtil;
import stroom.util.logging.StroomLogger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Script to create some base data for testing.
 */
public final class SetupSampleDataBean {
    private static final StroomLogger LOGGER = StroomLogger.getLogger(SetupSampleDataBean.class);

    private static final String ROOT_DIR_NAME = "samples";

    private static final String STATS_COUNT_FEED_NAME = "COUNT_FEED";
    private static final String STATS_VALUE_FEED_NAME = "VALUE_FEED";

    private static final int LOAD_CYCLES = 10;

    @Resource
    private DBRealm dbRealm;
    @Resource
    private FeedService feedService;
    @Resource
    private StreamStore streamStore;
    @Resource
    private StreamAttributeKeyService streamAttributeKeyService;
    @Resource
    private CommonTestControl commonTestControl;
    @Resource
    private ImportExportSerializer importExportSerializer;
    @Resource
    private StreamProcessorFilterService streamProcessorFilterService;
    @Resource
    private StreamProcessorService streamProcessorService;
    @Resource
    private PipelineService pipelineEntityService;
    @Resource
    private VolumeService volumeService;
    @Resource
    private IndexService indexService;
    @Resource
    private JobService jobService;
    @Resource
    private JobNodeService jobNodeService;

    @Resource
    private StatisticStoreService statisticsDataSourceService;

    public SetupSampleDataBean() {
    }

    private void createStreamAttributes() {
        final BaseResultList<StreamAttributeKey> list = streamAttributeKeyService
                .find(new FindStreamAttributeKeyCriteria());
        final HashSet<String> existingItems = new HashSet<String>();
        for (final StreamAttributeKey streamAttributeKey : list) {
            existingItems.add(streamAttributeKey.getName());
        }
        for (final String name : StreamAttributeConstants.SYSTEM_ATTRIBUTE_FIELD_TYPE_MAP.keySet()) {
            if (!existingItems.contains(name)) {
                try {
                    streamAttributeKeyService.save(new StreamAttributeKey(name,
                            StreamAttributeConstants.SYSTEM_ATTRIBUTE_FIELD_TYPE_MAP.get(name)));
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void run(final boolean shutdown) throws IOException {
        // Ensure admin user exists.
        LOGGER.info("Creating admin user");
        dbRealm.createOrRefreshAdmin();

        // Sample data/config can exist in many projects so here we define all
        // the root directories that we want to
        // process
        final File[] rootDirs = new File[] { new File(StroomCoreServerTestFileUtil.getTestResourcesDir(), ROOT_DIR_NAME),
                new File("./stroom-statistics-server/src/test/resources", ROOT_DIR_NAME) };

        // process each root dir in turn
        for (final File dir : rootDirs) {
            loadDirectory(shutdown, dir);
        }

        generateSampleStatisticsData();

        // code to check that the statisticsDataSource objects are stored
        // correctly
        final BaseResultList<StatisticStoreEntity> statisticsDataSources = statisticsDataSourceService
                .find(FindStatisticsEntityCriteria.instance());

        for (final StatisticStoreEntity statisticsDataSource : statisticsDataSources) {
            LOGGER.info(String.format("Retreiving statisticsDataSource with name: %s, engine: %s and type: %s",
                    statisticsDataSource.getName(), statisticsDataSource.getEngineName(),
                    statisticsDataSource.getStatisticType()));
        }

        // Add volumes to all indexes.
        final BaseResultList<Volume> volumeList = volumeService.find(new FindVolumeCriteria());
        final BaseResultList<Index> indexList = indexService.find(new FindIndexCriteria());
        final Set<Volume> volumeSet = new HashSet<Volume>(volumeList);

        for (final Index index : indexList) {
            index.setVolumes(volumeSet);
            indexService.save(index);

            // Find the pipeline for this index.
            final BaseResultList<PipelineEntity> pipelines = pipelineEntityService
                    .find(new FindPipelineEntityCriteria(index.getName()));

            if (pipelines == null || pipelines.size() == 0) {
                LOGGER.warn("No pipeline found for index '" + index.getName() + "'");
            } else if (pipelines.size() > 1) {
                LOGGER.warn("More than 1 pipeline found for index '" + index.getName() + "'");
            } else {
                final PipelineEntity pipeline = pipelines.getFirst();

                // Create a processor for this index.
                final FindStreamCriteria criteria = new FindStreamCriteria();
                criteria.obtainStreamTypeIdSet().add(StreamType.EVENTS);
                streamProcessorFilterService.createNewFilter(pipeline, criteria, true, 10);
                // final StreamProcessorFilter filter =
                // streamProcessorFilterService.createNewFilter(pipeline,
                // criteria, true, 10);
                //
                // // Enable the filter.
                // filter.setEnabled(true);
                // streamProcessorFilterService.save(filter);
                //
                // // Enable the processor.
                // final StreamProcessor streamProcessor =
                // filter.getStreamProcessor();
                // streamProcessor.setEnabled(true);
                // streamProcessorService.save(streamProcessor);
            }
        }

        final List<Feed> feeds = feedService.find(new FindFeedCriteria());

        // Create stream processors for all feeds.
        for (final Feed feed : feeds) {
            // Find the pipeline for this feed.
            final BaseResultList<PipelineEntity> pipelines = pipelineEntityService
                    .find(new FindPipelineEntityCriteria(feed.getName()));

            if (pipelines == null || pipelines.size() == 0) {
                LOGGER.warn("No pipeline found for feed '" + feed.getName() + "'");
            } else if (pipelines.size() > 1) {
                LOGGER.warn("More than 1 pipeline found for feed '" + feed.getName() + "'");
            } else {
                final PipelineEntity pipeline = pipelines.getFirst();

                // Create a processor for this feed.
                final FindStreamCriteria criteria = new FindStreamCriteria();
                criteria.obtainFeeds().obtainInclude().add(feed);
                criteria.obtainStreamTypeIdSet().add(StreamType.RAW_EVENTS);
                criteria.obtainStreamTypeIdSet().add(StreamType.RAW_REFERENCE);
                streamProcessorFilterService.createNewFilter(pipeline, criteria, true, 10);
                // final StreamProcessorFilter filter =
                // streamProcessorFilterService.createNewFilter(pipeline,
                // criteria, true, 10);
                //
                // // Enable the filter.
                // filter.setEnabled(true);
                // streamProcessorFilterService.save(filter);
                //
                // // Enable the processor.
                // final StreamProcessor streamProcessor =
                // filter.getStreamProcessor();
                // streamProcessor.setEnabled(true);
                // streamProcessorService.save(streamProcessor);
            }
        }

        try (final Connection connection = ConnectionUtil.getConnection()) {
            new V6_0_0_1__Explorer().migrate(connection);
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
        }

        if (shutdown) {
            commonTestControl.shutdown();
        }
    }

    public void loadDirectory(final boolean shutdown, final File importRootDir) throws IOException {
        LOGGER.info("Loading sample data for directory: " + importRootDir.getAbsolutePath());

        final File configDir = new File(importRootDir, "config");
        final File dataDir = new File(importRootDir, "input");

        createStreamAttributes();

        if (configDir.exists()) {
            // Load config.
            importExportSerializer.read(configDir, null, ImportMode.IGNORE_CONFIRMATION);

            // Enable all flags for all feeds.
            final List<Feed> feeds = feedService.find(new FindFeedCriteria());
            for (final Feed feed : feeds) {
                feed.setStatus(FeedStatus.RECEIVE);
                feedService.save(feed);
            }

            LOGGER.info("Node count = " + commonTestControl.countEntity(Node.class));
            LOGGER.info("Volume count = " + commonTestControl.countEntity(Volume.class));
            LOGGER.info("Feed count = " + commonTestControl.countEntity(Feed.class));
            LOGGER.info("StreamAttributeKey count = " + commonTestControl.countEntity(StreamAttributeKey.class));
            LOGGER.info("Dashboard count = " + commonTestControl.countEntity(Dashboard.class));
            LOGGER.info("Pipeline count = " + commonTestControl.countEntity(PipelineEntity.class));
            LOGGER.info("Index count = " + commonTestControl.countEntity(Index.class));
            LOGGER.info("StatisticDataSource count = " + commonTestControl.countEntity(StatisticStore.class));

        } else {
            LOGGER.info(String.format("Directory %s doesn't exist so skipping", configDir));
        }

        if (dataDir.exists()) {
            // Load data.
            final DataLoader dataLoader = new DataLoader(feedService, streamStore);

            // We spread the received time over 10 min intervals to help test
            // repo
            // layout start 2 weeks ago.
            final long dayMs = 1000 * 60 * 60 * 24;
            final long tenMinMs = 1000 * 60 * 10;
            long startTime = System.currentTimeMillis() - (14 * dayMs);

            // Load each data item 10 times to create a reasonable amount to
            // test.
            final Feed fd = dataLoader.getFeed("DATA_SPLITTER-EVENTS");
            for (int i = 0; i < LOAD_CYCLES; i++) {
                // Load reference data first.
                dataLoader.read(dataDir, true, startTime);
                startTime += tenMinMs;

                // Then load event data.
                dataLoader.read(dataDir, false, startTime);
                startTime += tenMinMs;

                // Load some randomly generated data.
                final String randomData = createRandomData();
                dataLoader.loadInputStream(fd, "Gen data", StreamUtil.stringToStream(randomData), false, startTime);
                startTime += tenMinMs;
            }
        } else {
            LOGGER.info(String.format("Directory %s doesn't exist so skipping", dataDir));
        }

        // streamTaskCreator.doCreateTasks();

        // // Add an index.
        // final Index index = addIndex();
        // addUserSearch(index);
        // addDictionarySearch(index);

    }

    /**
     * Generates some sample statistics data in two feeds. If the feed doesn't
     * exist it will fail silently
     */
    private void generateSampleStatisticsData() {
        final DataLoader dataLoader = new DataLoader(feedService, streamStore);
        final long startTime = System.currentTimeMillis();

        // count stats data
        try {
            final Feed countFeed = dataLoader.getFeed(STATS_COUNT_FEED_NAME);

            dataLoader.loadInputStream(countFeed, "Auto generated statistics count data",
                    StreamUtil.stringToStream(GenerateSampleStatisticsData.generateCountData()), false, startTime);
        } catch (final RuntimeException e1) {
            LOGGER.warn(String.format("Feed %s does not exist so cannot load the sample count statistics data.",
                    STATS_COUNT_FEED_NAME));
        }

        // value stats data
        try {
            final Feed valueFeed = dataLoader.getFeed(STATS_VALUE_FEED_NAME);

            dataLoader.loadInputStream(valueFeed, "Auto generated statistics value data",
                    StreamUtil.stringToStream(GenerateSampleStatisticsData.generateValueData()), false, startTime);
        } catch (final RuntimeException e) {
            LOGGER.warn(String.format("Feed %s does not exist so cannot load the sample value statistics data.",
                    STATS_VALUE_FEED_NAME));
        }

    }

    private String createRandomData() {
        final DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy,HH:mm:ss").withZone(DateTimeZone.UTC);
        final DateTime refDateTime = new DateTime(2010, 1, 1, 0, 0, 0);

        final StringBuilder sb = new StringBuilder();
        sb.append("Date,Time,FileNo,LineNo,User,Message\n");

        for (int i = 0; i < 1000; i++) {
            final DateTime dateTime = refDateTime.plus((long) (Math.random() * 10000000));
            sb.append(formatter.print(dateTime));
            sb.append(",");
            sb.append(createNum(4));
            sb.append(",");
            sb.append(createNum(10));
            sb.append(",user");
            sb.append(createNum(10));
            sb.append(",Some message ");
            sb.append(createNum(10));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String createNum(final int max) {
        return String.valueOf((int) (Math.random() * max) + 1);
    }

    // private Folder getOrCreate(String name) {
    // Folder parentGroup = null;
    // Folder folder = null;
    //
    // String[] parts = name.split("/");
    // for (String part : parts) {
    // parentGroup = folder;
    // folder = folderService.loadByName(parentGroup, part);
    // }
    // return folder;
    // }
    //
    // private Index addIndex() {
    // try {
    // final Folder folder = getOrCreate("Indexes/Example index");
    // final Pipeline indexTranslation = findTranslation("Example index");
    // return setupIndex(folder, "Example index", indexTranslation);
    //
    // } catch (final IOException e) {
    // throw new RuntimeException(e.getMessage(), e);
    // }
    // }
    //
    // private Pipeline findTranslation(final String name) {
    // final FindPipelineCriteria findTranslationCriteria = new
    // FindPipelineCriteria();
    // findTranslationCriteria.setName(name);
    // final BaseResultList<Pipeline> list = pipelineService
    // .find(findTranslationCriteria);
    // if (list != null && list.size() > 0) {
    // return list.getFirst();
    // }
    //
    // throw new RuntimeException("No translation found with name: " + name);
    // }
    //
    // private XSLT findXSLT(final String name) {
    // final FindXSLTCriteria findXSLTCriteria = new FindXSLTCriteria();
    // findXSLTCriteria.setName(name);
    // final BaseResultList<XSLT> list = xsltService.find(findXSLTCriteria);
    // if (list != null && list.size() > 0) {
    // return list.getFirst();
    // }
    //
    // throw new RuntimeException("No translation found with name: " + name);
    // }
    //
    // private Index setupIndex(final Folder folder,
    // final String indexName, final Pipeline indexTranslation)
    // throws IOException {
    // Index index = new Index();
    // index.setFolder(folder);
    // index.setName(indexName);
    //
    // index = indexService.save(index);
    //
    // return index;
    // }
    //
    // private void addUserSearch(final Index index) {
    // final Folder folder = getOrCreate(SEARCH + "/Search Examples");
    // final XSLT resultXSLT = findXSLT("Search Result Table - Show XML");
    //
    // final SearchExpressionTerm content1 = new SearchExpressionTerm();
    // content1.setField("UserId");
    // content1.setValue("userone");
    // final SearchExpressionOperator andOperator = new
    // SearchExpressionOperator(
    // true);
    // andOperator.addChild(content1);
    //
    // // FIXME : Set result pipeline.
    // final Search expression = new Search(index, null, andOperator);
    // expression.setName("User search");
    // expression.setFolder(folder);
    // searchExpressionService.save(expression);
    //
    // final Dictionary dictionary = new Dictionary();
    // dictionary.setName("User list");
    // dictionary.setWords("userone\nuser1");
    // }
    //
    // private void addDictionarySearch(final Index index) {
    // final Folder folder = getOrCreate(SEARCH + "/Search Examples");
    // final XSLT resultXSLT = findXSLT("Search Result Table - Show XML");
    //
    // final Dictionary dictionary = new Dictionary();
    // dictionary.setName("User list");
    // dictionary.setWords("userone\nuser1");
    // dictionary.setFolder(folder);
    //
    // dictionaryService.save(dictionary);
    //
    // final SearchExpressionTerm content1 = new SearchExpressionTerm();
    // content1.setField("UserId");
    // content1.setOperator(Operator.IN_DICTIONARY);
    // content1.setValue("User list");
    // final SearchExpressionOperator andOperator = new
    // SearchExpressionOperator(
    // true);
    // andOperator.addChild(content1);
    //
    // // FIXME : Set result pipeline.
    // final Search expression = new Search(index, null, andOperator);
    // expression.setName("Dictionary search");
    // expression.setFolder(folder);
    //
    // searchExpressionService.save(expression);
    // }
}
