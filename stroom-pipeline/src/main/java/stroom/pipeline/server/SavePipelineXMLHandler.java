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

import stroom.entity.server.MarshalOptions;
import stroom.pipeline.shared.PipelineEntity;
import stroom.pipeline.shared.PipelineService;
import stroom.pipeline.shared.SavePipelineXMLAction;
import stroom.task.server.AbstractTaskHandler;
import stroom.task.server.TaskHandlerBean;
import stroom.util.shared.VoidResult;

import javax.annotation.Resource;

@TaskHandlerBean(task = SavePipelineXMLAction.class)
public class SavePipelineXMLHandler extends AbstractTaskHandler<SavePipelineXMLAction, VoidResult> {
    @Resource
    private PipelineService pipelineEntityService;
    @Resource
    private MarshalOptions marshalOptions;

    @Override
    public VoidResult exec(final SavePipelineXMLAction action) {
        marshalOptions.setDisabled(true);

        final PipelineEntity pipelineEntity = pipelineEntityService.loadByUuid(action.getPipeline().getUuid());

        if (pipelineEntity != null) {
            pipelineEntity.setData(action.getXml());
            pipelineEntityService.save(pipelineEntity);
        }

        return VoidResult.INSTANCE;
    }
}
