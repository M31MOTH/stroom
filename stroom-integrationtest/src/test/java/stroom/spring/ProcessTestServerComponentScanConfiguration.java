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

package stroom.spring;

import stroom.pipeline.server.PipelineServiceImpl;
import stroom.util.logging.StroomLogger;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import stroom.dashboard.server.QueryServiceImpl;
import stroom.security.server.UserServiceImpl;
import stroom.DatabaseCommonTestControl;
import stroom.cluster.server.ClusterNodeManagerImpl;
import stroom.dictionary.DictionaryServiceImpl;
import stroom.feed.server.FeedServiceImpl;
import stroom.folder.server.FolderServiceImpl;
import stroom.importexport.server.ImportExportServiceImpl;
import stroom.jobsystem.server.ClusterLockServiceImpl;
import stroom.jobsystem.server.JobManagerImpl;
import stroom.jobsystem.server.JobNodeServiceImpl;
import stroom.jobsystem.server.JobServiceImpl;
import stroom.jobsystem.server.ScheduleServiceImpl;
import stroom.node.server.GlobalPropertyServiceImpl;
import stroom.node.server.NodeConfigImpl;
import stroom.node.server.NodeServiceImpl;
import stroom.node.server.RecordCountServiceImpl;
import stroom.pipeline.server.TextConverterServiceImpl;
import stroom.pipeline.server.XSLTServiceImpl;
import stroom.resource.server.ResourceStoreImpl;
import stroom.statistics.server.common.MetaDataStatisticImpl;
import stroom.statistics.server.common.StatisticEventStoreFactoryImpl;
import stroom.streamstore.server.StreamAttributeKeyServiceImpl;
import stroom.streamstore.server.StreamTypeServiceImpl;
import stroom.streamstore.server.fs.FileSystemStreamStore;
import stroom.streamtask.server.StreamProcessorFilterServiceImpl;
import stroom.streamtask.server.StreamProcessorServiceImpl;
import stroom.streamtask.server.StreamProcessorTaskFactory;
import stroom.streamtask.server.StreamTaskCreatorImpl;
import stroom.streamtask.server.StreamTaskServiceImpl;
import stroom.volume.server.VolumeServiceImpl;
import stroom.xmlschema.server.XMLSchemaServiceImpl;

/**
 * Configures the context for process integration tests.
 *
 * Reuses production configurations but defines its own component scan.
 *
 * This configuration relies on @ActiveProfile(StroomSpringProfiles.TEST) being
 * applied to the tests.
 */
/**
 * Exclude other configurations that might be found accidentally during a
 * component scan as configurations should be specified explicitly.
 */
@Configuration
@ComponentScan(basePackages = { "stroom" }, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = Configuration.class),

        // Exclude these so we get the mocks instead.
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ClusterLockServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ClusterNodeManagerImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = DatabaseCommonTestControl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = DictionaryServiceImpl.class),
        // @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value =
        // EntityPathResolverImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = FeedServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = FileSystemStreamStore.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = GlobalPropertyServiceImpl.class),
        // @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value =
        // ImportExportSerializerImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ImportExportServiceImpl.class),
        // @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value =
        // IndexServiceImpl.class),
        // @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value =
        // IndexShardServiceImpl.class),
        // @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value =
        // IndexShardWriterCacheImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = JobManagerImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = JobNodeServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = JobServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = MetaDataStatisticImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = NodeConfigImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = NodeServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = PipelineServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = QueryServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = RecordCountServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ResourceStoreImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = ScheduleServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = StatisticEventStoreFactoryImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = StreamAttributeKeyServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = StreamProcessorFilterServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = StreamProcessorServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = StreamProcessorTaskFactory.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = StreamTaskCreatorImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = StreamTaskServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = StreamTypeServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = FolderServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = UserServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = TextConverterServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = VolumeServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = XMLSchemaServiceImpl.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = XSLTServiceImpl.class) })
public class ProcessTestServerComponentScanConfiguration {
    private static final StroomLogger LOGGER = StroomLogger.getLogger(ProcessTestServerComponentScanConfiguration.class);

    public ProcessTestServerComponentScanConfiguration() {
        LOGGER.info("ProcessConfiguration loading...");
    }
}
