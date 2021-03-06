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

package stroom.app.client.gin;

import stroom.about.client.AboutPlugin;
import stroom.core.client.gin.PluginModule;
import stroom.entity.client.EntityPluginEventManager;
import stroom.help.client.HelpPlugin;

public class PluginsModule extends PluginModule {
    @Override
    protected void configure() {
        bindPlugin(EntityPluginEventManager.class);

        bindPlugin(HelpPlugin.class);
        bindPlugin(AboutPlugin.class);
    }
}
