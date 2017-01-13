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

package stroom.importexport.server;

import stroom.AbstractCoreIntegrationTest;
import stroom.CommonTestControl;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.EntityActionConfirmation;
import stroom.entity.shared.FindFolderCriteria;
import stroom.entity.shared.Folder;
import stroom.entity.shared.FolderService;
import stroom.feed.shared.Feed;
import stroom.feed.shared.FeedService;
import stroom.pipeline.shared.PipelineEntity;
import stroom.pipeline.shared.PipelineService;
import stroom.resource.server.ResourceStore;
import stroom.util.shared.ResourceKey;
import stroom.util.test.FileSystemTestUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.List;

public class TestImportExportServiceImpl extends AbstractCoreIntegrationTest {
    @Resource
    private ImportExportService importExportService;
    @Resource
    private ResourceStore resourceStore;
    @Resource
    private FolderService folderService;
    @Resource
    private PipelineService pipelineEntityService;
    @Resource
    private FeedService feedService;
    @Resource
    private CommonTestControl commonTestControl;

    @Test
    public void testExport() {
        // commonTestControl.deleteAll();

        Assert.assertEquals(0, commonTestControl.countEntity(Folder.class));

        Folder folder1 = folderService.create(null, "Root1_" + FileSystemTestUtil.getUniqueTestString());
        folder1 = folderService.save(folder1);
        final DocRef folder1Ref = DocRef.create(folder1);

        Folder folder2 = folderService.create(null, "Root2_" + FileSystemTestUtil.getUniqueTestString());
        folder2 = folderService.save(folder2);
        final DocRef folder2Ref = DocRef.create(folder2);

        Folder folder2child1 = folderService.create(folder2Ref, "Root2_Child1_" + FileSystemTestUtil.getUniqueTestString());
        folder2child1 = folderService.save(folder2child1);
        final DocRef folder2child1Ref = DocRef.create(folder2child1);

        Folder folder2child2 = folderService.create(folder2Ref, "Root2_Child2_" + FileSystemTestUtil.getUniqueTestString());
        folder2child2 = folderService.save(folder2child2);
        final DocRef folder2child2Ref = DocRef.create(folder2child2);

        Assert.assertEquals(4, commonTestControl.countEntity(Folder.class));

        final PipelineEntity tran1 = pipelineEntityService.create(folder1Ref, FileSystemTestUtil.getUniqueTestString());
        tran1.setDescription("Description");
        pipelineEntityService.save(tran1);

        PipelineEntity tran2 = pipelineEntityService.create(folder2Ref, FileSystemTestUtil.getUniqueTestString());
        tran2.setDescription("Description");
        tran2.setParentPipeline(DocRef.create(tran1));
        tran2 = pipelineEntityService.save(tran2);

        final Feed referenceFeed = feedService.create(folder1Ref, FileSystemTestUtil.getUniqueTestString());
        referenceFeed.setDescription("Description");
        feedService.save(referenceFeed);

        Feed eventFeed = feedService.create(folder2Ref, FileSystemTestUtil.getUniqueTestString());
        eventFeed.setDescription("Description");
        // eventFeed.getReferenceFeed().add(referenceFeed);
        eventFeed = feedService.save(eventFeed);

        final Feed eventFeedChild = feedService.create(folder2child1Ref, FileSystemTestUtil.getUniqueTestString());
        eventFeedChild.setDescription("Description");
        // eventFeedChild.getReferenceFeed().add(referenceFeed);
        feedService.save(eventFeedChild);

        final int startFolderSize = commonTestControl.countEntity(Folder.class);
        final int startTranslationSize = commonTestControl.countEntity(PipelineEntity.class);
        final int startFeedSize = commonTestControl.countEntity(Feed.class);

        final ResourceKey file = resourceStore.createTempFile("Export.zip");
        final FindFolderCriteria criteria = new FindFolderCriteria();
        criteria.getFolderIdSet().add(folder1);
        criteria.getFolderIdSet().add(folder2);

        // Export
        importExportService.exportConfig(criteria, resourceStore.getTempFile(file), false, null);

        final ResourceKey exportConfig = resourceStore.createTempFile("ExportPlain.zip");

        importExportService.exportConfig(criteria, resourceStore.getTempFile(exportConfig), false, null);

        // Delete it and check
        pipelineEntityService.delete(tran2);
        Assert.assertEquals(startTranslationSize - 1, commonTestControl.countEntity(PipelineEntity.class));

        feedService.delete(eventFeed);
        Assert.assertEquals(startFeedSize - 1, commonTestControl.countEntity(Feed.class));

        Assert.assertEquals(4, commonTestControl.countEntity(Folder.class));

        // Import
        final List<EntityActionConfirmation> confirmations = importExportService
                .createImportConfirmationList(resourceStore.getTempFile(file));

        for (final EntityActionConfirmation confirmation : confirmations) {
            confirmation.setAction(true);
        }

        importExportService.performImportWithConfirmation(resourceStore.getTempFile(file), confirmations);

        Assert.assertEquals(startFolderSize, commonTestControl.countEntity(Folder.class));
        Assert.assertEquals(startFeedSize, commonTestControl.countEntity(Feed.class));
        Assert.assertEquals(startTranslationSize, commonTestControl.countEntity(PipelineEntity.class));

        final ResourceKey fileChild = resourceStore.createTempFile("ExportChild.zip");
        final FindFolderCriteria criteriaChild = new FindFolderCriteria();
        criteriaChild.getFolderIdSet().add(folder2child2);

        // Export
        importExportService.exportConfig(criteriaChild, resourceStore.getTempFile(fileChild), false, null);
    }
}
