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

import jenkins.plugins.jclouds.config.ConfigExport;

/**
 * Exports an all jclouds UserData to xml on stdout
 *
 * @author Fritz Elfert
 */
@Extension
public class JCloudsGetUserdataCommand extends CLICommand {

    @Argument(required = false, metaVar = "CREDENTIAL", usage = "ID of credential (RSA key) to encrypt data")
    public String cred = null;

    @Override
    public String getShortDescription() {
        return Messages.GetUserdataCommand_shortDescription();
    }

    @Override
    protected int run() throws IOException, CmdLineException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        stdout.println(CliHelper.XML_HEADER);
        stdout.println(new ConfigExport(cred).exportXml());
        return 0;
    }
}
