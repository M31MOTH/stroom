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

package stroom.pipeline.server;

import org.springframework.context.annotation.Scope;
import stroom.pipeline.server.factory.ElementRegistryFactory;
import stroom.pipeline.shared.FetchPropertyTypesAction;
import stroom.pipeline.shared.FetchPropertyTypesResult;
import stroom.task.server.AbstractTaskHandler;
import stroom.task.server.TaskHandlerBean;
import stroom.util.spring.StroomScope;

import javax.inject.Inject;

@TaskHandlerBean(task = FetchPropertyTypesAction.class)
@Scope(value = StroomScope.TASK)
public class FetchPropertyTypesHandler extends AbstractTaskHandler<FetchPropertyTypesAction, FetchPropertyTypesResult> {
    private final ElementRegistryFactory pipelineElementRegistryFactory;

    @Inject
    FetchPropertyTypesHandler(final ElementRegistryFactory pipelineElementRegistryFactory) {
        this.pipelineElementRegistryFactory = pipelineElementRegistryFactory;
    }

    @Override
    public FetchPropertyTypesResult exec(final FetchPropertyTypesAction action) {
        return new FetchPropertyTypesResult(pipelineElementRegistryFactory.get().getPropertyTypes());
    }
}
