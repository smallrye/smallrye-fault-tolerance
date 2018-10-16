/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.faulttolerance;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

/**
 * Default implementation of {@link CommandListenersProvider}. Note that dependent listeners are not destroyed automatically.
 *
 * @author mkouba
 */
@ApplicationScoped
public class DefaultCommandListenersProvider implements CommandListenersProvider {

    @Inject
    Instance<CommandListener> listeners;

    public List<CommandListener> getCommandListeners() {
        if (listeners.isUnsatisfied()) {
            return null;
        } else {
            return StreamSupport.stream(listeners.spliterator(), false).sorted().collect(Collectors.toList());
        }
    }

}
