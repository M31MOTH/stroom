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

package stroom.index.server;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import stroom.entity.server.MockDocumentService;
import stroom.entity.shared.DocumentType;
import stroom.index.shared.FindIndexCriteria;
import stroom.index.shared.Index;
import stroom.index.shared.IndexService;
import stroom.util.spring.StroomSpringProfiles;

@Profile(StroomSpringProfiles.TEST)
@Component("indexService")
public class MockIndexService extends MockDocumentService<Index, FindIndexCriteria> implements IndexService {
    @Override
    public DocumentType getDocumentType() {
        return getDocumentType(10, "Index", "Index");
    }

    @Override
    public Class<Index> getEntityClass() {
        return Index.class;
    }
}
