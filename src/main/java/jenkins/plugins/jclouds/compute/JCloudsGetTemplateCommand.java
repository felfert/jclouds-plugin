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

import java.io.IOException;

import hudson.Extension;
import hudson.cli.CLICommand;
import jenkins.model.Jenkins;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;

/**
 * Exports an existing template to xml on stdout
 *
 * @author Fritz Elfert
 */
@Extension
public class JCloudsGetTemplateCommand extends CLICommand {

    @Argument(required = false, metaVar = "PROFILE", index = 1, usage = "Name of jclouds profile to use")
        public String profile = null;

    @Argument(required = true, metaVar = "TEMPLATE", index = 0, usage = "Name of template to export")
        public String tmpl;

    @Override
    public String getShortDescription() {
        return Messages.GetTemplateCommand_shortDescription();
    }

    @Override
    protected int run() throws IOException, CmdLineException {
        Jenkins.get().checkPermission(Jenkins.READ);
        final JCloudsCloud c = CliHelper.resolveCloud(profile);
        final JCloudsSlaveTemplate tpl = CliHelper.resolveTemplate(c, tmpl);
        stdout.println(tpl.asXml());
        return 0;
    }

}
