/*
 * Copyright 2023 Fritz Elfert
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
package jenkins.plugins.jclouds.compute;

import java.util.ArrayList;
import java.util.List;

import hudson.slaves.Cloud;
import hudson.util.EditDistance;

import jenkins.model.Jenkins;

import org.kohsuke.args4j.CmdLineException;

class CliHelper {

    static public JCloudsCloud resolveCloud(String name) throws CmdLineException {
        if (null != name) {

            final Jenkins.CloudList cl = Jenkins.get().clouds;
            final Cloud c = cl.getByName(name);
            if (null != c && c instanceof JCloudsCloud) {
                return (JCloudsCloud)c;
            }
            final List<String> names = new ArrayList<>();
            for (final Cloud cloud : Jenkins.get().clouds) {
                if (cloud instanceof JCloudsCloud) {
                    String n = ((JCloudsCloud)cloud).profile;
                    if (n.length() > 0) {
                        names.add(n);
                    }
                }
            }
            throw new CmdLineException(null, MyMsg.NO_SUCH_PROFILE_EXISTS,
                name, EditDistance.findNearest(name, names));
        }
        return null;
    }

    static public List<String> getAllTemplateNames(JCloudsCloud cloud) {
        final List<String> ret = new ArrayList<>();
        if (null == cloud) {
            for (final Cloud c : Jenkins.get().clouds) {
                if (c instanceof JCloudsCloud) {
                    final JCloudsCloud jc = (JCloudsCloud)c;
                    for (final JCloudsSlaveTemplate t : jc.getTemplates()) {
                        if (!ret.contains(t.name)) {
                            ret.add(t.name);
                        }
                    }
                }
            }
            return ret;
        }
        for (final JCloudsSlaveTemplate t : cloud.getTemplates()) {
            ret.add(t.name);
        }
        return ret;
    }

    static public JCloudsSlaveTemplate resolveTemplate(JCloudsCloud cloud, String name) throws CmdLineException {
        JCloudsSlaveTemplate ret = null;
        if (null == cloud) {
            for (final Cloud c : Jenkins.get().clouds) {
                if (c instanceof JCloudsCloud) {
                    final JCloudsCloud jc = (JCloudsCloud)c;
                    JCloudsSlaveTemplate t = jc.getTemplate(name);
                    if (null != t) {
                        if (null != ret) {
                            throw new CmdLineException(null, MyMsg.AMBIGUOUS_TEMPLATE, name);
                        }
                        ret = t;
                    }
                }
            }
        } else {
            ret = cloud.getTemplate(name);
        }
        if (null == ret) {
            List<String> names = getAllTemplateNames(cloud);
            throw new CmdLineException(null, MyMsg.NO_SUCH_TEMPLATE_EXISTS, name, EditDistance.findNearest(name, names));
        }
        return ret;
    }

}
